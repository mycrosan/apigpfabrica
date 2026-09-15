"""Seleção de motores locais e encaminhamento ao Paddle sem carregar seus pesos."""
import http.client
import ipaddress
import json
import logging
import math
import os
import socket
import time
from threading import Event, Timer
from urllib.parse import urlsplit, urlunsplit

LOGGER = logging.getLogger(__name__)
CAMPOS = frozenset(('MARCA', 'MODELO', 'MEDIDA', 'PAIS', 'DOT'))
LIMITE_RESPOSTA = 4 * 1024 * 1024


def endpoint_loopback(endereco):
    """Resolve localhost sem DNS e recusa destinos que possam enviar fotos para fora."""
    partes = urlsplit(endereco)
    if (partes.scheme != 'http' or not partes.hostname or '%' in partes.netloc
            or partes.username is not None or partes.password is not None
            or partes.query or partes.fragment or partes.path not in ('', '/')):
        raise ValueError('Endpoint Paddle deve ser HTTP local, sem credenciais ou caminho')
    host = '127.0.0.1' if partes.hostname == 'localhost' else partes.hostname
    try:
        endereco_ip = ipaddress.ip_address(host)
        porta = partes.port
    except ValueError as erro:
        raise ValueError('Endpoint Paddle deve usar endereço loopback literal') from erro
    if not endereco_ip.is_loopback or porta == 0:
        raise ValueError('Endpoint Paddle deve usar endereço loopback literal')
    host = f'[{endereco_ip}]' if endereco_ip.version == 6 else str(endereco_ip)
    autoridade = f'{host}:{porta}' if porta is not None else host
    return urlunsplit(('http', autoridade, '', '', ''))


def _texto_preenchido(valor, limite=512):
    return isinstance(valor, str) and bool(valor.strip()) and len(valor) <= limite


def _numero_finito(valor):
    return type(valor) in (int, float) and math.isfinite(valor)


def validar_resposta_paddle(resposta, campo):
    """Preserva o contrato auditável e rejeita respostas de outro motor ou campo."""
    if not isinstance(resposta, dict) or resposta.get('motor') != 'PADDLEOCR' or resposta.get('campo') != campo:
        raise ValueError('Resposta incompatível do Paddle local')
    for atributo in ('versaoBiblioteca', 'versaoModelo', 'versaoPreprocessamento'):
        if not _texto_preenchido(resposta.get(atributo)):
            raise ValueError('Versão ausente na resposta do Paddle local')
    duracao = resposta.get('duracaoMs')
    linhas = resposta.get('linhas')
    if type(duracao) is not int or duracao < 0 or not isinstance(linhas, list) or len(linhas) > 1000:
        raise ValueError('Contrato inválido do Paddle local')
    for linha in linhas:
        if not isinstance(linha, dict) or not _texto_preenchido(linha.get('texto'), 4096):
            raise ValueError('Texto inválido do Paddle local')
        escore = linha.get('escore')
        regiao = linha.get('regiao')
        if not _numero_finito(escore) or not 0 <= escore <= 1:
            raise ValueError('Escore inválido do Paddle local')
        if not isinstance(regiao, list) or len(regiao) < 3 or len(regiao) > 100:
            raise ValueError('Região inválida do Paddle local')
        for ponto in regiao:
            if not isinstance(ponto, list) or len(ponto) != 2 or any(type(coordenada) is not int for coordenada in ponto):
                raise ValueError('Coordenadas inválidas do Paddle local')
    return resposta


class PaddleProxyLocal:
    def __init__(self):
        self.endpoint = endpoint_loopback(os.getenv('OCR_PADDLE_ENDPOINT', 'http://127.0.0.1:8091'))
        self.timeout = float(os.getenv('OCR_PADDLE_TIMEOUT_SECONDS', '12'))
        if not math.isfinite(self.timeout) or not 0 < self.timeout <= 12:
            raise ValueError('Timeout Paddle deve ser maior que zero e no máximo 12 segundos')

    def _requisitar(self, caminho, dados=None, timeout=None):
        token = os.getenv('OCR_TOKEN', '')
        if not token:
            raise ValueError('Token interno ausente')
        prazo = time.monotonic() + (timeout or self.timeout)
        corpo = json.dumps(dados).encode() if dados is not None else None
        partes = urlsplit(self.endpoint)
        conexao = http.client.HTTPConnection(partes.hostname, partes.port, timeout=self._tempo_restante(prazo))
        temporizador = None
        expirou = Event()
        try:
            conexao.connect()
            canal = conexao.sock

            def interromper():
                expirou.set()
                try:
                    canal.shutdown(socket.SHUT_RDWR)
                except OSError:
                    LOGGER.debug('Conexão Paddle já encerrada ao atingir o prazo')

            # Limita a chamada completa mesmo se o servidor entregar pequenos trechos continuamente.
            temporizador = Timer(self._tempo_restante(prazo), interromper)
            temporizador.daemon = True
            temporizador.start()
            conexao.request('POST' if dados is not None else 'GET', caminho, body=corpo, headers={
                'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json',
                'Accept': 'application/json', 'Connection': 'close',
            })
            with conexao.getresponse() as resposta:
                if resposta.status != 200:
                    raise RuntimeError('Paddle local indisponível')
                conteudo = resposta.read(LIMITE_RESPOSTA + 1)
            self._tempo_restante(prazo)
        except (OSError, http.client.HTTPException) as erro:
            if expirou.is_set() or isinstance(erro, TimeoutError):
                raise TimeoutError('Tempo limite do Paddle local excedido') from erro
            raise RuntimeError('Falha de comunicação com Paddle local') from erro
        finally:
            if temporizador is not None:
                temporizador.cancel()
            conexao.close()
        if len(conteudo) > LIMITE_RESPOSTA:
            raise ValueError('Resposta Paddle acima do limite')
        return json.loads(conteudo)

    @staticmethod
    def _tempo_restante(prazo):
        restante = prazo - time.monotonic()
        if restante <= 0:
            raise TimeoutError('Tempo limite do Paddle local excedido')
        return restante

    def pronta(self):
        resposta = self._requisitar('/ready', timeout=min(2, self.timeout))
        if (not isinstance(resposta, dict) or resposta.get('status') != 'pronto'
                or not _texto_preenchido(resposta.get('versaoModelo'))):
            raise RuntimeError('Paddle local não provisionado')
        return True

    def executar(self, campo, foto_base64):
        if campo not in CAMPOS or not foto_base64:
            raise ValueError('Campo e foto original são obrigatórios no encaminhamento')
        resposta = self._requisitar('/v1/reconhecer', {'campo': campo, 'foto_base64': foto_base64})
        return validar_resposta_paddle(resposta, campo)


def executar_motor(motor, imagem, campo, foto_base64=None):
    if isinstance(motor, MotorRoteado):
        return motor.executar(imagem, campo, foto_base64)
    inicio = time.monotonic()
    linhas = motor.reconhecer(imagem, campo)
    return {
        'motor': getattr(motor, 'motor', 'PADDLEOCR'),
        'versaoBiblioteca': getattr(motor, 'versao_biblioteca', '3.3.2'),
        'versaoModelo': motor.versao, 'campo': campo,
        'versaoPreprocessamento': motor.preprocessamento(campo),
        'linhas': linhas, 'duracaoMs': round((time.monotonic() - inicio) * 1000),
    }


class MotorRoteado:
    versao = 'roteamento-local-v1'

    def __init__(self, fabrica_ollama, fabrica_paddle=PaddleProxyLocal):
        configurados = os.getenv('OLLAMA_CAMPOS', 'MODELO').split(',')
        self.campos_ollama = frozenset(campo.strip().upper() for campo in configurados)
        if not self.campos_ollama or not self.campos_ollama <= CAMPOS:
            raise ValueError('OLLAMA_CAMPOS contém campo vazio ou desconhecido')
        self.paddle = fabrica_paddle()
        self.ollama = None
        try:
            self.ollama = fabrica_ollama()
        except Exception as erro:
            # A indisponibilidade do experimento não desabilita campos do Paddle já provisionado.
            LOGGER.error('Ollama local indisponível ao iniciar: %s', type(erro).__name__)

    def pronta(self):
        self.paddle.pronta()
        if self.ollama is None or not self.ollama.pronta():
            raise RuntimeError('Ollama local não provisionado')
        return True

    def executar(self, imagem, campo, foto_base64=None):
        if campo not in CAMPOS:
            raise ValueError('Campo desconhecido')
        if campo not in self.campos_ollama:
            return self.paddle.executar(campo, foto_base64)
        if self.ollama is None:
            raise RuntimeError('Ollama local não provisionado')
        return executar_motor(self.ollama, imagem, campo)


def selecionar_motor(diretorio, diretorio_relevo=None):
    configuracao = os.getenv('OCR_MOTOR', 'paddleocr').strip().lower()
    if configuracao == 'paddleocr':
        from .engine import MotorLocal
        return MotorLocal(diretorio, diretorio_relevo)
    if configuracao in ('ollama', 'hibrido'):
        from .ollama_engine import MotorOllama
        return MotorOllama() if configuracao == 'ollama' else MotorRoteado(MotorOllama)
    raise ValueError('OCR_MOTOR deve ser paddleocr, ollama ou hibrido')
