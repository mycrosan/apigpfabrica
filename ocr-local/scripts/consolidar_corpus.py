"""Consolida cópias e vínculos sem transformar cadastro histórico em verdade de treino."""
import argparse
from collections import Counter, defaultdict
import csv
import hashlib
import json
from pathlib import Path
import sqlite3


def consolidar(diretorio):
    resumo_inventario = json.loads((diretorio / 'resumo.json').read_text())
    vinculos = {}
    with (diretorio / 'vinculos_cadastro_nao_revisado.csv').open() as entrada:
        for linha in csv.DictReader(entrada):
            vinculos[linha['arquivo_nome']] = json.loads(linha['carcaca_ids'])
    banco = sqlite3.connect(f'file:{diretorio / "inventario.sqlite"}?mode=ro', uri=True)
    execucao = banco.execute('SELECT MAX(execucao) FROM fotos').fetchone()[0]
    quantidade = banco.execute('SELECT COUNT(*) FROM fotos WHERE execucao = ?', (execucao,)).fetchone()[0]
    if quantidade != resumo_inventario['quantidade_arquivos']:
        banco.close()
        raise ValueError('Inventário ainda incompleto; aguarde a conclusão')
    origens = defaultdict(list)
    for (dados,) in banco.execute('SELECT dados FROM fotos WHERE execucao = ?', (execucao,)):
        registro = json.loads(dados)
        if registro['sha256']:
            origens[registro['sha256']].append(registro)
    banco.close()
    estados = Counter()
    grupos = set()
    destino = diretorio / 'corpus_nao_revisado.jsonl'
    with destino.open('w') as saida:
        for checksum, registros in sorted(origens.items()):
            identificadores = set()
            for registro in registros:
                identificadores.update(vinculos.get(Path(registro['arquivo']).name.lower(), []))
            grupos.update(identificadores)
            estado = 'SEM_VINCULO'
            if len(identificadores) == 1:
                estado = 'REFERENCIADO_NAO_VERIFICADO'
            if len(identificadores) > 1:
                estado = 'CONFLITANTE'
            estados[estado] += 1
            primeiro = registros[0]
            dados = {'imagem_sha256': checksum, 'arquivos': sorted(r['arquivo'] for r in registros),
                     'grupos_candidatos': [f'gppremium:carcaca:{identificador}' for identificador in sorted(identificadores)],
                     'estado_grupo': estado, 'estado_imagem': primeiro['estado'],
                     'largura': primeiro['largura'], 'altura': primeiro['altura'],
                     'possui_gps': any(r['possui_gps'] for r in registros),
                     'origem': 'LEGADO_NAO_REVISADO', 'revisao': 'PENDENTE', 'elegivel_treino': False,
                     'grupo_fisico_validado': None, 'particao': None, 'transcricao_validada': None}
            saida.write(json.dumps(dados, ensure_ascii=False, sort_keys=True) + '\n')
    resumo = {'imagens_unicas': len(origens), 'estados_grupo': dict(estados), 'grupos_candidatos_distintos': len(grupos),
              'manifesto_sha256': hashlib.sha256(destino.read_bytes()).hexdigest(),
              'rotulos_aprovados': 0, 'treino_iniciado': False, 'particoes_criadas': False,
              'observacao': 'Aprovar transcrição/região e conferir vínculo físico antes de separar treino, validação e teste.'}
    (diretorio / 'resumo_corpus.json').write_text(json.dumps(resumo, ensure_ascii=False, indent=2) + '\n')
    return resumo


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--diretorio', required=True, type=Path)
    print(json.dumps(consolidar(parser.parse_args().diretorio), ensure_ascii=False))
