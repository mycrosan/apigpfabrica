"""Instala/remove os dois serviços locais de desenvolvimento no login do usuário."""
import argparse
import os
import plistlib
import subprocess
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1]


def gerenciar(acao):
    if sys.platform != 'darwin':
        raise RuntimeError('Este comando exige macOS')
    dominio = f'gui/{os.getuid()}'
    pasta = Path.home() / 'Library/LaunchAgents'
    for servico in ('ollama', 'gateway'):
        rotulo = f'br.com.gpfabrica.ocr.{servico}'
        destino = pasta / f'{rotulo}.plist'
        if acao == 'parar':
            subprocess.run(['launchctl', 'bootout', f'{dominio}/{rotulo}'], check=True)
            destino.unlink(missing_ok=True)
            continue
        if destino.exists():
            raise FileExistsError(f'Serviço já configurado: {destino}; pare antes de reinstalar')
        pasta.mkdir(parents=True, exist_ok=True)
        (RAIZ / 'ollama-local').mkdir(exist_ok=True)
        conteudo = {'Label': rotulo, 'ProgramArguments': [str(RAIZ / '.venv-ollama/bin/python'),
                    str(RAIZ / 'scripts/iniciar_ollama_local.py'), servico],
                    'WorkingDirectory': str(RAIZ), 'RunAtLoad': True, 'KeepAlive': True,
                    'ThrottleInterval': 30,
                    'StandardOutPath': str(RAIZ / f'ollama-local/{servico}.log'),
                    'StandardErrorPath': str(RAIZ / f'ollama-local/{servico}.log')}
        destino.write_bytes(plistlib.dumps(conteudo))
        destino.chmod(0o600)
        subprocess.run(['launchctl', 'bootstrap', dominio, str(destino)], check=True)
        print(f'Serviço instalado: {rotulo}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('acao', choices=['instalar', 'parar'])
    gerenciar(parser.parse_args().acao)
