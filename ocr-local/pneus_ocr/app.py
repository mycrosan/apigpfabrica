"""API interna: autenticação, limites e falhas explícitas antes da inferência."""
import base64
import binascii
import hmac
import io
import logging
import os
import warnings
from contextlib import asynccontextmanager
from pathlib import Path
from typing import Literal

from fastapi import FastAPI, HTTPException, Request
from PIL import Image, ImageOps, UnidentifiedImageError
from pydantic import BaseModel, ConfigDict, Field, ValidationError
from starlette.concurrency import run_in_threadpool

from .gateway import executar_motor, selecionar_motor

LOGGER = logging.getLogger(__name__)
LIMITE_BYTES = int(os.getenv('OCR_MAX_BYTES', '8388608'))
LIMITE_PIXELS = int(os.getenv('OCR_MAX_PIXELS', '20000000'))
LIMITE_HTTP = ((LIMITE_BYTES + 2) // 3) * 4 + 8192
Image.MAX_IMAGE_PIXELS = LIMITE_PIXELS


class Entrada(BaseModel):
    model_config = ConfigDict(extra='forbid')
    campo: Literal['MARCA', 'MODELO', 'MEDIDA', 'PAIS', 'DOT']
    foto_base64: str = Field(min_length=1, max_length=LIMITE_HTTP)


def decodificar(texto: str):
    if texto.startswith('data:'):
        partes = texto.split(',', 1)
        if len(partes) != 2:
            raise HTTPException(400, 'Imagem inválida')
        texto = partes[1]
    try:
        dados = base64.b64decode(texto, validate=True)
        if len(dados) > LIMITE_BYTES:
            raise HTTPException(413, 'Imagem acima do limite')
        with warnings.catch_warnings():
            warnings.simplefilter('error', Image.DecompressionBombWarning)
            with Image.open(io.BytesIO(dados)) as imagem:
                if imagem.format not in ('JPEG', 'PNG', 'WEBP'):
                    raise HTTPException(400, 'Formato não suportado')
                if imagem.width * imagem.height > LIMITE_PIXELS:
                    raise HTTPException(413, 'Imagem acima do limite de pixels')
                imagem.load()
                return ImageOps.exif_transpose(imagem).convert('RGB')
    except (binascii.Error, ValueError, OSError, UnidentifiedImageError) as erro:
        raise HTTPException(400, 'Não foi possível decodificar a imagem') from erro
    except (Image.DecompressionBombError, Image.DecompressionBombWarning) as erro:
        raise HTTPException(413, 'Imagem acima do limite de pixels') from erro


def criar_app(fabrica_motor=selecionar_motor):
    @asynccontextmanager
    async def lifespan(aplicacao):
        aplicacao.state.motor = None
        if os.getenv('OCR_TOKEN'):
            try:
                diretorio_relevo = os.getenv('OCR_MODELOS_RELEVO_DIR')
                argumentos = [Path(os.getenv('OCR_MODELS_DIR', '/models'))]
                if diretorio_relevo:
                    argumentos.append(Path(diretorio_relevo))
                aplicacao.state.motor = await run_in_threadpool(fabrica_motor, *argumentos)
            except Exception as erro:
                LOGGER.error('OCR indisponível ao carregar pesos: %s', type(erro).__name__)
        try:
            yield
        finally:
            motor = aplicacao.state.motor
            aplicacao.state.motor = None
            if motor is not None and hasattr(motor, 'fechar'):
                await run_in_threadpool(motor.fechar)

    aplicacao = FastAPI(lifespan=lifespan, docs_url=None, redoc_url=None)

    @aplicacao.get('/health')
    def health():
        return {'status': 'ativo'}

    @aplicacao.get('/ready')
    def ready():
        if aplicacao.state.motor is None:
            raise HTTPException(503, 'OCR não provisionado')
        try:
            if hasattr(aplicacao.state.motor, 'pronta') and not aplicacao.state.motor.pronta():
                raise RuntimeError('Motor local não está pronto')
        except Exception as erro:
            LOGGER.warning('OCR indisponível na verificação: %s', type(erro).__name__)
            raise HTTPException(503, 'OCR não provisionado') from erro
        return {'status': 'pronto', 'versaoModelo': aplicacao.state.motor.versao}

    @aplicacao.post('/v1/reconhecer')
    async def reconhecer(request: Request):
        token = os.getenv('OCR_TOKEN', '')
        recebido = request.headers.get('authorization', '')
        if not token or not hmac.compare_digest(recebido, 'Bearer ' + token):
            raise HTTPException(403, 'Acesso não autorizado')
        if aplicacao.state.motor is None:
            raise HTTPException(503, 'OCR indisponível; utilize o cadastro manual')
        corpo = bytearray()
        async for parte in request.stream():
            if len(corpo) + len(parte) > LIMITE_HTTP:
                raise HTTPException(413, 'Requisição acima do limite')
            corpo.extend(parte)
        try:
            entrada = Entrada.model_validate_json(bytes(corpo))
        except ValidationError as erro:
            raise HTTPException(400, 'Contrato de leitura inválido') from erro
        imagem = decodificar(entrada.foto_base64)
        try:
            return await run_in_threadpool(executar_motor, aplicacao.state.motor, imagem,
                                           entrada.campo, entrada.foto_base64)
        except BlockingIOError as erro:
            raise HTTPException(503, 'OCR ocupado; tente novamente') from erro
        except Exception as erro:
            LOGGER.error('Falha de inferência: %s', type(erro).__name__)
            raise HTTPException(503, 'OCR indisponível; utilize o cadastro manual') from erro
        finally:
            imagem.close()

    return aplicacao


app = criar_app()
