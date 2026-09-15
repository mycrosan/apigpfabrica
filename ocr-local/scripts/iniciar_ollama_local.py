"""Inicia cada processo no Mac com acesso de rede restrito ao próprio host."""
import argparse
import os
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1]
PERFIL = '(version 1) (allow default) (deny network-outbound) (allow network-outbound (remote ip "localhost:*"))'


def iniciar(servico):
    if sys.platform != 'darwin':
        raise RuntimeError('Este inicializador exige macOS; use isolamento equivalente no servidor Linux')
    ambiente = os.environ.copy()
    ambiente.update(OLLAMA_NO_CLOUD='1', OLLAMA_HOST='127.0.0.1:11434',
                    OLLAMA_NUM_PARALLEL='1', OLLAMA_MAX_LOADED_MODELS='1',
                    OLLAMA_MAX_QUEUE='1', OLLAMA_CONTEXT_LENGTH='2048')
    if servico == 'ollama':
        comando = ['/opt/homebrew/bin/ollama', 'serve']
    else:
        valores = {}
        for linha in (RAIZ / '.env').read_text().splitlines():
            if linha.strip() and not linha.lstrip().startswith('#') and '=' in linha:
                chave, valor = linha.split('=', 1)
                valores[chave.strip()] = valor.strip().strip('"').strip("'")
        if not valores.get('OCR_TOKEN'):
            raise ValueError('Configure OCR_TOKEN em ocr-local/.env')
        ambiente.update(OCR_TOKEN=valores['OCR_TOKEN'], OCR_MOTOR='hibrido',
                        OLLAMA_CAMPOS=valores.get('OLLAMA_CAMPOS', 'MODELO'),
                        OLLAMA_ENDPOINT='http://127.0.0.1:11434',
                        OLLAMA_MODELO=valores.get('OLLAMA_MODELO', 'gemma3:4b'),
                        OLLAMA_MANIFESTO=str(RAIZ / 'ollama-local/manifest.json'),
                        OCR_PADDLE_ENDPOINT='http://127.0.0.1:8091')
        # launchd inicia os processos independentemente; aguarda a porta antes do
        # carregamento dos pesos, para não fixar uma falha causada pela ordem de subida.
        cliente = urllib.request.build_opener(urllib.request.ProxyHandler({}))
        for tentativa in range(60):
            try:
                with cliente.open('http://127.0.0.1:11434/api/version', timeout=1):
                    break
            except (urllib.error.URLError, OSError):
                if tentativa == 59:
                    raise RuntimeError('Ollama não iniciou; consulte ollama-local/ollama.log')
                time.sleep(1)
        comando = [str(RAIZ / '.venv-ollama/bin/python'), '-m', 'uvicorn',
                   'pneus_ocr.app:app', '--host', '127.0.0.1', '--port', '8092', '--workers', '1']
    os.chdir(RAIZ)
    os.execve('/usr/bin/sandbox-exec', ['sandbox-exec', '-p', PERFIL, *comando], ambiente)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('servico', choices=['ollama', 'gateway'])
    iniciar(parser.parse_args().servico)
