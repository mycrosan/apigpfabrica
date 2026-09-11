"""Ensaio pareado de modelos locais; extração e latência não medem acurácia."""
import argparse
import hashlib
import importlib.metadata
import json
import math
import statistics
import time
from pathlib import Path

from PIL import Image, ImageOps
from pneus_ocr.engine import MotorLocal


def executar(modelos: Path, entradas: Path, saida: Path):
    lista = json.loads(entradas.read_text())
    if not isinstance(lista, list) or not lista:
        raise ValueError('Informe uma lista não vazia de fotos para o ensaio')
    # Falhar antes de carregar pesos evita produzir um relatório aparentemente completo.
    for item in lista:
        if item['campo_ensaio'] not in ('DOT', 'MARCA', 'MODELO', 'MEDIDA', 'PAIS'):
            raise ValueError('Campo de ensaio desconhecido')
        if not Path(item['arquivo']).is_file():
            raise FileNotFoundError(item['arquivo'])
    saida.mkdir(parents=True, exist_ok=False)
    motor = MotorLocal(modelos)
    resultados = []
    with (saida / 'leituras_nao_revisadas.jsonl').open('x') as arquivo:
        for indice, item in enumerate(lista):
            origem = Path(item['arquivo'])
            registro = {
                'arquivo': str(origem),
                'imagem_sha256': hashlib.sha256(origem.read_bytes()).hexdigest(),
                'campo_ensaio': item['campo_ensaio'],
                'campo_rotulado': None,
                'rotulo_aprovado': False,
                'elegivel_treino': False,
            }
            inicio = time.monotonic()
            try:
                with Image.open(origem) as original:
                    imagem = ImageOps.exif_transpose(original).convert('RGB')
                try:
                    registro['linhas'] = motor.reconhecer(imagem, item['campo_ensaio'])
                    registro['estado_tecnico'] = 'CONCLUIDO'
                finally:
                    imagem.close()
            except Exception as erro:
                registro['estado_tecnico'] = 'FALHA'
                registro['tipo_erro'] = type(erro).__name__
            registro['duracao_ms'] = round((time.monotonic() - inicio) * 1000, 2)
            registro['versao_preprocessamento'] = motor.preprocessamento(item['campo_ensaio'])
            arquivo.write(json.dumps(registro, ensure_ascii=False) + '\n')
            arquivo.flush()
            resultados.append(registro)
            print(json.dumps({'foto': indice + 1, 'total': len(lista),
                              'estado': registro['estado_tecnico'],
                              'duracao_ms': registro['duracao_ms']}), flush=True)
    tempos = sorted(r['duracao_ms'] for r in resultados if r['estado_tecnico'] == 'CONCLUIDO')
    resumo = {
        'versao_modelo': motor.versao,
        'manifesto': json.loads((modelos / 'manifest.json').read_text()),
        'bibliotecas': {nome: importlib.metadata.version(nome)
                       for nome in ('paddleocr', 'paddlepaddle', 'pillow', 'numpy', 'opencv-contrib-python')},
        'fotos': len(resultados),
        'falhas': sum(r['estado_tecnico'] == 'FALHA' for r in resultados),
        'fotos_com_texto': sum(any(l['texto'].strip() for l in r.get('linhas', [])) for r in resultados),
        'latencia_p50_ms': statistics.median(tempos) if tempos else None,
        'latencia_p95_ms': tempos[math.ceil(len(tempos) * .95) - 1] if tempos else None,
        'acuracia': None,
        'observacao': 'Ensaio exploratório sem rótulos aprovados. Latência inclui abertura da foto e primeira inferência sem aquecimento. Não aprova implantação.',
    }
    (saida / 'resumo.json').write_text(json.dumps(resumo, ensure_ascii=False, indent=2) + '\n')
    return resumo['falhas'] == 0


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--modelos', type=Path, required=True)
    parser.add_argument('--entradas', type=Path, required=True)
    parser.add_argument('--saida', type=Path, required=True)
    args = parser.parse_args()
    raise SystemExit(0 if executar(args.modelos, args.entradas, args.saida) else 1)
