import base64
import io
import json
import sys
import time
from contextlib import contextmanager
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from threading import Thread
from types import SimpleNamespace
from unittest.mock import Mock, patch

import pytest
from PIL import Image

from pneus_ocr.gateway import (
    LIMITE_RESPOSTA, MotorRoteado, PaddleProxyLocal, endpoint_loopback,
    executar_motor, selecionar_motor, validar_resposta_paddle,
)


def resposta_paddle(campo='MEDIDA'):
    return {
        'motor': 'PADDLEOCR', 'versaoBiblioteca': '3.3.2', 'versaoModelo': 'pesos-paddle',
        'versaoPreprocessamento': 'exif-transpose-rgb-relevo-v1', 'campo': campo,
        'linhas': [{'texto': '205/55R16', 'escore': 0.9,
                    'regiao': [[0, 0], [20, 0], [20, 20], [0, 20]]}], 'duracaoMs': 123,
    }


class OllamaTeste:
    motor = 'OLLAMA_LOCAL'
    versao_biblioteca = 'ollama-teste'
    versao = 'digest-ollama'

    def pronta(self):
        return True

    def preprocessamento(self, campo):
        return 'vlm-rgb-v1'

    def reconhecer(self, imagem, campo):
        return [{'texto': 'HF201', 'escore': None, 'regiao': []}]


@contextmanager
def servidor_local(responder):
    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            responder(self, None)

        def do_POST(self):
            dados = json.loads(self.rfile.read(int(self.headers['Content-Length'])))
            responder(self, dados)

        def log_message(self, *args):
            pass

    servidor = ThreadingHTTPServer(('127.0.0.1', 0), Handler)
    thread = Thread(target=lambda: servidor.serve_forever(poll_interval=0.01), daemon=True)
    thread.start()
    try:
        yield f'http://127.0.0.1:{servidor.server_port}'
    finally:
        servidor.shutdown()
        servidor.server_close()
        thread.join(timeout=2)


def responder_json(handler, conteudo, status=200):
    handler.send_response(status)
    handler.send_header('Content-Type', 'application/json')
    handler.end_headers()
    handler.wfile.write(json.dumps(conteudo).encode())


@pytest.mark.parametrize('url', [
    'https://127.0.0.1:8091', 'http://10.0.0.1:8091', 'http://ollama.com',
    'http://localhost.exemplo.com', 'http://127.0.0.1@exemplo.com',
    'http://segredo@127.0.0.1', 'http://127.0.0.1/path',
    'http://127.0.0.1?destino=externo', 'http://127.0.0.1#fragmento',
    'http://127.0.0.1:0', 'file:///tmp/servico',
    'http://@127.0.0.1', 'http://[::1%lo0]:8091',
])
def test_rejeita_endpoint_que_nao_seja_http_loopback(url):
    with pytest.raises(ValueError):
        endpoint_loopback(url)


@pytest.mark.parametrize(('url', 'esperado'), [
    ('http://localhost:8091/', 'http://127.0.0.1:8091'),
    ('http://127.0.0.1:8091', 'http://127.0.0.1:8091'),
    ('http://[::1]:8091', 'http://[::1]:8091'),
])
def test_permite_apenas_loopback_sem_consultar_dns(url, esperado):
    assert endpoint_loopback(url) == esperado


@pytest.mark.parametrize('timeout', ['0', '-1', '12.1', 'inf', 'nan'])
def test_timeout_nao_pode_exceder_orcamento(monkeypatch, timeout):
    monkeypatch.setenv('OCR_PADDLE_TIMEOUT_SECONDS', timeout)
    with pytest.raises(ValueError):
        PaddleProxyLocal()


def test_proxy_local_preserva_foto_credencial_e_metadados_ignorando_proxy_ambiente(monkeypatch):
    requisicoes = []
    imagem = io.BytesIO()
    Image.new('RGB', (16, 16)).save(imagem, 'PNG')
    foto = base64.b64encode(imagem.getvalue()).decode()

    def responder(handler, dados):
        requisicoes.append((handler.path, dados, handler.headers['Authorization']))
        if handler.path == '/ready':
            responder_json(handler, {'status': 'pronto', 'versaoModelo': 'pesos-paddle'})
        else:
            responder_json(handler, resposta_paddle())

    monkeypatch.setenv('OCR_TOKEN', 'token-local-teste')
    monkeypatch.setenv('http_proxy', 'http://127.0.0.1:1')
    monkeypatch.setenv('https_proxy', 'http://127.0.0.1:1')
    monkeypatch.setenv('no_proxy', '')
    with servidor_local(responder) as endpoint:
        monkeypatch.setenv('OCR_PADDLE_ENDPOINT', endpoint)
        proxy = PaddleProxyLocal()
        assert proxy.pronta()
        assert proxy.executar('MEDIDA', foto) == resposta_paddle()
    assert requisicoes == [
        ('/ready', None, 'Bearer token-local-teste'),
        ('/v1/reconhecer', {'campo': 'MEDIDA', 'foto_base64': foto}, 'Bearer token-local-teste'),
    ]


def test_proxy_recusa_redirecionamento_sem_repetir_foto(monkeypatch):
    requisicoes = []

    def responder(handler, dados):
        requisicoes.append(handler.path)
        handler.send_response(307)
        handler.send_header('Location', '/destino')
        handler.end_headers()

    monkeypatch.setenv('OCR_TOKEN', 'token-local-teste')
    with servidor_local(responder) as endpoint:
        monkeypatch.setenv('OCR_PADDLE_ENDPOINT', endpoint)
        with pytest.raises(RuntimeError):
            PaddleProxyLocal().executar('MEDIDA', 'sintetica')
    assert requisicoes == ['/v1/reconhecer']


@pytest.mark.parametrize(('atributo', 'valor'), [
    ('motor', 'OLLAMA_LOCAL'), ('campo', 'MODELO'), ('versaoModelo', ''),
    ('versaoPreprocessamento', None), ('versaoBiblioteca', None),
    ('duracaoMs', -1), ('duracaoMs', True), ('linhas', None),
])
def test_recusa_resposta_paddle_com_metadados_invalidos(atributo, valor):
    resposta = resposta_paddle()
    resposta[atributo] = valor
    with pytest.raises(ValueError):
        validar_resposta_paddle(resposta, 'MEDIDA')


@pytest.mark.parametrize(('atributo', 'valor'), [
    ('texto', ''), ('escore', None), ('escore', float('nan')),
    ('escore', 1.1), ('escore', True), ('regiao', []),
    ('regiao', [[0, 0], [20, 0], [20, float('inf')]]),
])
def test_recusa_linha_sem_evidencia_paddle_valida(atributo, valor):
    resposta = resposta_paddle()
    resposta['linhas'][0][atributo] = valor
    with pytest.raises(ValueError):
        validar_resposta_paddle(resposta, 'MEDIDA')


def test_resposta_excessiva_e_limitada_e_conexao_fechada(monkeypatch):
    monkeypatch.setenv('OCR_TOKEN', 'token-local-teste')
    resposta = Mock(status=200)
    resposta.read.return_value = b'x' * (LIMITE_RESPOSTA + 1)
    contexto = Mock()
    contexto.__enter__ = Mock(return_value=resposta)
    contexto.__exit__ = Mock(return_value=False)
    proxy = PaddleProxyLocal()
    with patch('pneus_ocr.gateway.http.client.HTTPConnection') as fabrica:
        fabrica.return_value.getresponse.return_value = contexto
        with pytest.raises(ValueError, match='acima do limite'):
            proxy.executar('MEDIDA', 'sintetica')
        fabrica.return_value.close.assert_called_once()
    resposta.read.assert_called_once_with(LIMITE_RESPOSTA + 1)
    contexto.__exit__.assert_called_once()


def test_prazo_total_interrompe_resposta_lenta_sem_repetir_chamada(monkeypatch):
    requisicoes = []

    def responder(handler, dados):
        requisicoes.append(handler.path)
        handler.send_response(200)
        handler.end_headers()
        for indice in range(20):
            try:
                handler.wfile.write(b' ')
                handler.wfile.flush()
            except OSError:
                return
            time.sleep(0.02)

    monkeypatch.setenv('OCR_TOKEN', 'token-local-teste')
    monkeypatch.setenv('OCR_PADDLE_TIMEOUT_SECONDS', '0.06')
    with servidor_local(responder) as endpoint:
        monkeypatch.setenv('OCR_PADDLE_ENDPOINT', endpoint)
        inicio = time.monotonic()
        with pytest.raises(TimeoutError):
            PaddleProxyLocal().executar('MEDIDA', 'sintetica')
        assert time.monotonic() - inicio < 0.3
    assert requisicoes == ['/v1/reconhecer']


def test_selecao_padrao_preserva_motor_paddle(monkeypatch):
    monkeypatch.delenv('OCR_MOTOR', raising=False)
    fabrica = Mock()
    with patch.dict(sys.modules, {'pneus_ocr.engine': SimpleNamespace(MotorLocal=fabrica)}):
        assert selecionar_motor('/models', '/relevo') is fabrica.return_value
    fabrica.assert_called_once_with('/models', '/relevo')


@pytest.mark.parametrize('selecao', ['hibrido', 'ollama'])
def test_selecao_ollama_nao_importa_paddle(monkeypatch, selecao):
    monkeypatch.setenv('OCR_MOTOR', selecao)
    monkeypatch.setenv('OLLAMA_CAMPOS', 'MODELO')
    with patch.dict(sys.modules, {
        'pneus_ocr.engine': None,
        'pneus_ocr.ollama_engine': SimpleNamespace(MotorOllama=OllamaTeste),
    }):
        motor = selecionar_motor('/models')
        assert isinstance(motor, MotorRoteado if selecao == 'hibrido' else OllamaTeste)


def test_selecao_desconhecida_falha_explicitamente(monkeypatch):
    monkeypatch.setenv('OCR_MOTOR', 'externo')
    with pytest.raises(ValueError):
        selecionar_motor('/models')


@pytest.mark.parametrize('campos', ['', 'MODELO,', 'MODELO,OUTRO'])
def test_campos_ollama_invalidos_nao_viram_fallback(monkeypatch, campos):
    monkeypatch.setenv('OLLAMA_CAMPOS', campos)
    with pytest.raises(ValueError):
        MotorRoteado(OllamaTeste, Mock())


def test_hibrido_seleciona_por_campo_preservando_auditoria_sem_mutar_estado(monkeypatch):
    monkeypatch.delenv('OLLAMA_CAMPOS', raising=False)
    paddle = Mock()
    paddle.executar.return_value = resposta_paddle()
    motor = MotorRoteado(OllamaTeste, lambda: paddle)
    with Image.new('RGB', (16, 16)) as imagem:
        primeira = executar_motor(motor, imagem, 'MODELO', 'original-modelo')
        segunda = executar_motor(motor, imagem, 'MEDIDA', 'original-medida')
        terceira = executar_motor(motor, imagem, 'MODELO', 'original-modelo')
    assert primeira['motor'] == terceira['motor'] == 'OLLAMA_LOCAL'
    assert primeira['versaoModelo'] == terceira['versaoModelo'] == 'digest-ollama'
    assert primeira['linhas'] == [{'texto': 'HF201', 'escore': None, 'regiao': []}]
    assert segunda == resposta_paddle()
    paddle.executar.assert_called_once_with('MEDIDA', 'original-medida')


def test_hibrido_preserva_paddle_se_ollama_falha_ao_iniciar(monkeypatch):
    monkeypatch.setenv('OLLAMA_CAMPOS', 'MODELO')
    paddle = Mock()
    paddle.executar.return_value = resposta_paddle()
    motor = MotorRoteado(Mock(side_effect=FileNotFoundError()), lambda: paddle)
    with pytest.raises(RuntimeError):
        motor.pronta()
    assert motor.executar(None, 'MEDIDA', 'original') == resposta_paddle()
    with pytest.raises(RuntimeError):
        motor.executar(None, 'MODELO', 'original')
    paddle.executar.assert_called_once_with('MEDIDA', 'original')


def test_falha_ollama_nao_repete_inferencia_no_paddle(monkeypatch):
    monkeypatch.setenv('OLLAMA_CAMPOS', 'MODELO')
    ollama = OllamaTeste()
    ollama.reconhecer = Mock(side_effect=TimeoutError('Tempo excedido'))
    paddle = Mock()
    motor = MotorRoteado(lambda: ollama, lambda: paddle)
    with pytest.raises(TimeoutError):
        motor.executar(None, 'MODELO', 'original')
    paddle.executar.assert_not_called()
