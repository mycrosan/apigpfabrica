"""Converte uma foto local em JSON para o OCR, sem conexão de rede."""
import argparse
import base64
import json
import os
import tempfile
from pathlib import Path


def ler_foto(foto, parser):
    try:
        if not foto.is_file():
            parser.error('A foto não foi encontrada.')
        if not 0 < foto.stat().st_size <= 8 * 1024 * 1024:
            parser.error('A foto deve ter conteúdo e no máximo 8 MiB.')
        dados = foto.read_bytes()
    except PermissionError:
        parser.error(
            'Sem permissão para ler a foto. No macOS, autorize o aplicativo '
            'que abriu este terminal (Terminal, iTerm ou editor) a acessar '
            'Downloads em Ajustes do Sistema > Privacidade e Segurança > '
            'Arquivos e Pastas. Depois, execute novamente o comando.')
    except OSError:
        parser.error('Não foi possível abrir a foto. Confira se o arquivo está disponível localmente.')
    if not 0 < len(dados) <= 8 * 1024 * 1024:
        parser.error('A foto deve ter conteúdo e no máximo 8 MiB.')
    return dados


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('foto', type=Path, help='Arquivo JPEG, PNG ou WEBP')
    parser.add_argument('--campo', default='DOT',
                        choices=['DOT', 'MARCA', 'MODELO', 'MEDIDA', 'PAIS'])
    args = parser.parse_args()
    if args.foto.suffix.lower() not in {'.jpg', '.jpeg', '.png', '.webp'}:
        parser.error('Use uma foto JPEG, PNG ou WEBP.')
    dados = ler_foto(args.foto, parser)

    diretorio = Path(__file__).resolve().parent / 'requisicoes'
    diretorio.mkdir(mode=0o700, exist_ok=True)
    corpo = {'campo': args.campo,
             'foto_base64': base64.b64encode(dados).decode('ascii')}
    # Um arquivo por execução preserva os testes anteriores sem sobrescrevê-los.
    descritor, nome = tempfile.mkstemp(prefix='foto_', suffix='.json', dir=diretorio)
    with os.fdopen(descritor, 'w', encoding='utf-8') as arquivo:
        json.dump(corpo, arquivo, ensure_ascii=False)
    print('Selecione este arquivo no Postman em Body > binary:')
    print(nome)


if __name__ == '__main__':
    main()
