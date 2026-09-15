import base64
import io
import os
from unittest.mock import patch

from fastapi.testclient import TestClient
from PIL import Image
from pneus_ocr.app import criar_app
from pneus_ocr.gateway import MotorRoteado


class MotorTeste:
    versao = 'pesos-teste'
    def __init__(self, diretorio):
        pass
    def preprocessamento(self, campo):
        return 'exif-transpose-rgb-v1'
    def reconhecer(self, imagem, campo=None):
        return [{'texto': '205/55R16', 'escore': 0.9, 'regiao': [[0, 0], [20, 0], [20, 20], [0, 20]]}]


def imagem_base64():
    arquivo = io.BytesIO()
    Image.new('RGB', (32, 32)).save(arquivo, format='PNG')
    return base64.b64encode(arquivo.getvalue()).decode()


def test_exige_token_mesmo_com_modelo_carregado():
    with patch.dict(os.environ, {'OCR_TOKEN': 'interno-teste'}):
        with TestClient(criar_app(MotorTeste)) as cliente:
            assert cliente.post('/v1/reconhecer', json={}).status_code == 403


def test_readiness_nao_simula_sucesso_sem_pesos():
    def falhar(diretorio):
        raise FileNotFoundError('Sem pesos')
    with patch.dict(os.environ, {'OCR_TOKEN': 'interno-teste'}):
        with TestClient(criar_app(falhar)) as cliente:
            assert cliente.get('/health').status_code == 200
            assert cliente.get('/ready').status_code == 503


def test_contrato_retorna_texto_sem_resolver_catalogo():
    with patch.dict(os.environ, {'OCR_TOKEN': 'interno-teste'}):
        with TestClient(criar_app(MotorTeste)) as cliente:
            resposta = cliente.post('/v1/reconhecer', headers={'Authorization': 'Bearer interno-teste'},
                json={'campo': 'MEDIDA', 'foto_base64': imagem_base64()})
            assert resposta.status_code == 200
            assert resposta.json()['linhas'][0]['texto'] == '205/55R16'
            assert resposta.json()['versaoModelo'] == 'pesos-teste'
            assert 'id' not in resposta.json()['linhas'][0]


def test_rejeita_base64_invalido_sem_expor_imagem():
    with patch.dict(os.environ, {'OCR_TOKEN': 'interno-teste'}):
        with TestClient(criar_app(MotorTeste)) as cliente:
            resposta = cliente.post('/v1/reconhecer', headers={'Authorization': 'Bearer interno-teste'},
                json={'campo': 'DOT', 'foto_base64': 'nao-e-imagem'})
            assert resposta.status_code == 400
            assert 'nao-e-imagem' not in resposta.text


def test_rejeita_campo_desconhecido():
    with patch.dict(os.environ, {'OCR_TOKEN': 'interno-teste'}):
        with TestClient(criar_app(MotorTeste)) as cliente:
            resposta = cliente.post('/v1/reconhecer', headers={'Authorization': 'Bearer interno-teste'},
                json={'campo': 'DESCONHECIDO', 'foto_base64': imagem_base64()})
            assert resposta.status_code == 400


def test_motor_ollama_expoe_versoes_reais_sem_inventar_escore():
    class MotorOllamaTeste(MotorTeste):
        motor = 'OLLAMA_LOCAL'
        versao_biblioteca = 'ollama-teste'
        versao = 'digest-pesos-vlm'

        def reconhecer(self, imagem, campo):
            return [{'texto': 'HF201', 'escore': None, 'regiao': []}]

    with patch.dict(os.environ, {'OCR_TOKEN': 'interno-teste'}):
        with TestClient(criar_app(MotorOllamaTeste)) as cliente:
            resposta = cliente.post('/v1/reconhecer', headers={'Authorization': 'Bearer interno-teste'},
                json={'campo': 'MODELO', 'foto_base64': imagem_base64()})
    assert resposta.status_code == 200
    assert resposta.json()['motor'] == 'OLLAMA_LOCAL'
    assert resposta.json()['versaoBiblioteca'] == 'ollama-teste'
    assert resposta.json()['versaoModelo'] == 'digest-pesos-vlm'
    assert resposta.json()['linhas'][0]['escore'] is None
    assert resposta.json()['linhas'][0]['regiao'] == []


def test_readiness_revalida_motor_depois_da_inicializacao():
    class MotorInvalido(MotorTeste):
        def pronta(self):
            raise RuntimeError('Pesos divergentes')

    with patch.dict(os.environ, {'OCR_TOKEN': 'interno-teste'}):
        with TestClient(criar_app(MotorInvalido)) as cliente:
            assert cliente.get('/health').status_code == 200
            assert cliente.get('/ready').status_code == 503


def test_falha_e_ocupacao_fecham_imagem_sem_expor_erro():
    import pytest
    for falha in (RuntimeError('conteudo-sensivel'), BlockingIOError('conteudo-sensivel')):
        imagens = []

        class MotorFalho(MotorTeste):
            def reconhecer(self, imagem, campo):
                imagens.append(imagem)
                raise falha

        with patch.dict(os.environ, {'OCR_TOKEN': 'interno-teste'}):
            with TestClient(criar_app(MotorFalho)) as cliente:
                resposta = cliente.post('/v1/reconhecer', headers={'Authorization': 'Bearer interno-teste'},
                    json={'campo': 'MODELO', 'foto_base64': imagem_base64()})
        assert resposta.status_code == 503
        assert 'conteudo-sensivel' not in resposta.text
        with pytest.raises(ValueError):
            imagens[0].getpixel((0, 0))


def test_hibrido_atende_paddle_mesmo_com_readiness_ollama_indisponivel():
    class PaddleTeste:
        def pronta(self):
            return True

        def executar(self, campo, foto_base64):
            assert foto_base64 == foto_original
            return {'motor': 'PADDLEOCR', 'campo': campo, 'linhas': []}

    def ollama_falho():
        raise FileNotFoundError('Modelo não provisionado')

    def fabrica(diretorio):
        return MotorRoteado(ollama_falho, PaddleTeste)

    foto_original = imagem_base64()
    with patch.dict(os.environ, {'OCR_TOKEN': 'interno-teste', 'OLLAMA_CAMPOS': 'MODELO'}):
        with TestClient(criar_app(fabrica)) as cliente:
            assert cliente.get('/ready').status_code == 503
            medida = cliente.post('/v1/reconhecer', headers={'Authorization': 'Bearer interno-teste'},
                json={'campo': 'MEDIDA', 'foto_base64': foto_original})
            modelo = cliente.post('/v1/reconhecer', headers={'Authorization': 'Bearer interno-teste'},
                json={'campo': 'MODELO', 'foto_base64': foto_original})
    assert medida.status_code == 200
    assert medida.json()['motor'] == 'PADDLEOCR'
    assert modelo.status_code == 503
