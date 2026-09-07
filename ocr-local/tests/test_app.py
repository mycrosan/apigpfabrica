import base64
import io
import os
from unittest.mock import patch

from fastapi.testclient import TestClient
from PIL import Image
from pneus_ocr.app import criar_app


class MotorTeste:
    versao = 'pesos-teste'
    def __init__(self, diretorio):
        pass
    def reconhecer(self, imagem):
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
