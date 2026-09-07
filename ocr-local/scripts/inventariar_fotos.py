"""Inventário local reproduzível. Não altera originais nem aprova rótulos de treino."""
import argparse
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
import hashlib
import io
import json
from pathlib import Path
import sqlite3
import time
import warnings

from PIL import Image, ImageOps

Image.MAX_IMAGE_PIXELS = 20_000_000
EXTENSOES = {'.jpg', '.jpeg', '.png', '.webp'}


def examinar(tarefa):
    raiz, arquivo = tarefa
    relativo = str(arquivo.relative_to(raiz))
    resultado = {'arquivo': relativo, 'pasta': str(arquivo.parent.relative_to(raiz)),
                 'bytes': 0, 'sha256': None, 'estado': 'INVALIDA', 'motivo': None,
                 'largura': None, 'altura': None, 'formato': None, 'orientacao': None,
                 'possui_gps': False, 'hash_visual': None}
    if arquivo.is_symlink() or not arquivo.resolve().is_relative_to(raiz.resolve()):
        resultado['motivo'] = 'LINK_OU_CAMINHO_EXTERNO'
        return resultado
    try:
        dados = arquivo.read_bytes()
        resultado.update(bytes=len(dados), sha256=hashlib.sha256(dados).hexdigest())
        with warnings.catch_warnings():
            warnings.simplefilter('error', Image.DecompressionBombWarning)
            with Image.open(io.BytesIO(dados)) as imagem:
                resultado.update(largura=imagem.width, altura=imagem.height, formato=imagem.format)
                if len(dados) > 8 * 1024 * 1024 or imagem.width * imagem.height > Image.MAX_IMAGE_PIXELS:
                    resultado['motivo'] = 'ACIMA_LIMITE_API'
                    return resultado
                if imagem.format not in {'JPEG', 'PNG', 'WEBP'}:
                    resultado['motivo'] = 'FORMATO_NAO_SUPORTADO'
                    return resultado
                imagem.load()
                exif = imagem.getexif()
                resultado['orientacao'] = exif.get(274)
                resultado['possui_gps'] = 34853 in exif
                # Hash de triagem; sem eliminar fotos parecidas que podem diferir em um dígito.
                reduzida = ImageOps.exif_transpose(imagem).convert('L').resize((9, 8))
                pixels = list(reduzida.getdata())
                bits = ''.join('1' if pixels[linha * 9 + coluna] > pixels[linha * 9 + coluna + 1] else '0'
                               for linha in range(8) for coluna in range(8))
                resultado.update(estado='DECODIFICAVEL', hash_visual=f'{int(bits, 2):016x}')
    except (OSError, ValueError, SyntaxError, Image.DecompressionBombWarning, Image.DecompressionBombError) as erro:
        resultado['motivo'] = type(erro).__name__
    return resultado


def inventariar(raiz, destino, trabalhadores=4):
    raiz = raiz.resolve()
    if not raiz.is_dir():
        raise ValueError('Diretório de fotos ausente')
    destino.mkdir(parents=True, exist_ok=True)
    arquivos = sorted(p for p in raiz.rglob('*') if p.is_file() and p.suffix.lower() in EXTENSOES)
    banco = sqlite3.connect(destino / 'inventario.sqlite')
    banco.execute('CREATE TABLE IF NOT EXISTS fotos (arquivo TEXT PRIMARY KEY, sha256 TEXT, estado TEXT, dados TEXT, execucao INTEGER)')
    banco.execute('CREATE INDEX IF NOT EXISTS ix_fotos_sha ON fotos(sha256)')
    execucao = time.time_ns()
    with ThreadPoolExecutor(max_workers=trabalhadores) as executor:
        for indice, registro in enumerate(executor.map(examinar, ((raiz, p) for p in arquivos)), 1):
            banco.execute('INSERT OR REPLACE INTO fotos VALUES (?, ?, ?, ?, ?)',
                          (registro['arquivo'], registro['sha256'], registro['estado'], json.dumps(registro, ensure_ascii=False), execucao))
            if indice % 1000 == 0:
                banco.commit()
                print(json.dumps({'processadas': indice, 'total': len(arquivos)}), flush=True)
    banco.commit()
    estados = Counter()
    motivos = Counter()
    pastas = Counter()
    dimensoes = Counter()
    hashes = set()
    quantidade = 0
    total_bytes = 0
    gps = 0
    sem_hash = 0
    validas_unicas = 0
    with (destino / 'manifesto.jsonl').open('w') as manifesto:
        for (serializado,) in banco.execute('SELECT dados FROM fotos WHERE execucao = ? ORDER BY arquivo', (execucao,)):
            registro = json.loads(serializado)
            quantidade += 1
            estados[registro['estado']] += 1
            motivos[registro['motivo']] += 1
            pastas[registro['pasta']] += 1
            dimensoes[f"{registro['largura']}x{registro['altura']}"] += 1
            total_bytes += registro['bytes']
            gps += registro['possui_gps']
            chave = registro['sha256']
            sem_hash += chave is None
            if chave is None or chave in hashes:
                continue
            hashes.add(chave)
            if registro['estado'] == 'DECODIFICAVEL':
                validas_unicas += 1
            registro.update(origem='LEGADO_NAO_REVISADO', revisao='PENDENTE', elegivel_treino=False,
                            grupo_fisico=None, particao=None, transcricao_validada=None)
            manifesto.write(json.dumps(registro, ensure_ascii=False, sort_keys=True) + '\n')
    dados_manifesto = (destino / 'manifesto.jsonl').read_bytes()
    resumo = {'versao_inventario': 1, 'origem': str(raiz), 'quantidade_arquivos': quantidade,
              'bytes': total_bytes, 'imagens_unicas_sha256': len(hashes), 'copias_exatas': quantidade - sem_hash - len(hashes), 'arquivos_sem_hash': sem_hash,
              'decodificaveis_unicas': validas_unicas, 'estados': dict(estados), 'motivos': dict(motivos),
              'arquivos_com_gps': gps, 'pastas': dict(pastas), 'dimensoes_frequentes': dimensoes.most_common(12),
              'manifesto_sha256': hashlib.sha256(dados_manifesto).hexdigest(),
              'rotulos_aprovados': 0, 'particoes_criadas': False,
              'observacao': 'Decodificável não significa legível. Sem revisão humana e grupo físico não há treino ou teste válido.'}
    (destino / 'resumo.json').write_text(json.dumps(resumo, ensure_ascii=False, indent=2) + '\n')
    banco.close()
    return resumo


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--origem', type=Path, required=True)
    parser.add_argument('--destino', type=Path, required=True)
    parser.add_argument('--trabalhadores', type=int, default=4, choices=range(1, 9))
    argumentos = parser.parse_args()
    print(json.dumps(inventariar(argumentos.origem, argumentos.destino, argumentos.trabalhadores), ensure_ascii=False))
