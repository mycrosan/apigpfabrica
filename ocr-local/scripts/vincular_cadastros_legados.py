"""Extrai vínculos foto–carcaça de dump local sem executar SQL nem aprovar transcrições."""
import argparse
import csv
import hashlib
import json
from pathlib import Path
import re


def linhas_insert(texto):
    campos = []
    campo = []
    aspas = False
    escapado = False
    dentro = False
    for caractere in texto:
        if escapado:
            campo.append(caractere)
            escapado = False
            continue
        if aspas and caractere == '\\':
            campo.append(caractere)
            escapado = True
            continue
        if caractere == "'":
            aspas = not aspas
            campo.append(caractere)
            continue
        if aspas:
            campo.append(caractere)
            continue
        if caractere == '(':
            if dentro:
                raise ValueError('Expressão SQL não suportada; revisar o exportador')
            dentro = True
            campos = []
            campo = []
            continue
        if dentro and caractere == ',':
            campos.append(''.join(campo).strip())
            campo = []
            continue
        if dentro and caractere == ')':
            campos.append(''.join(campo).strip())
            yield campos
            dentro = False
            campo = []
            continue
        if dentro:
            campo.append(caractere)
    if dentro or aspas:
        raise ValueError('INSERT incompleto')


def extrair(dump, destino):
    colunas = []
    estrutura = False
    vinculos = {}
    carcacas = 0
    tamanho = dump.stat().st_size
    if tamanho > 2 * 1024**3:
        raise ValueError('Dump acima do limite de análise local de 2 GiB')
    with dump.open(encoding='utf-8', errors='strict') as entrada:
        for linha in entrada:
            if linha.startswith('CREATE TABLE `carcaca`'):
                estrutura = True
                continue
            if estrutura and linha.startswith(')'):
                estrutura = False
            if estrutura:
                coluna = re.match(r'\s*`([^`]+)`', linha)
                if coluna:
                    colunas.append(coluna.group(1))
            prefixo = 'INSERT INTO `carcaca` VALUES '
            if not linha.startswith(prefixo):
                continue
            if not {'id', 'fotos'} <= set(colunas):
                raise ValueError('Estrutura da tabela carcaça ausente ou incompatível')
            for valores in linhas_insert(linha[len(prefixo):]):
                if len(valores) != len(colunas):
                    raise ValueError('Quantidade de colunas diferente da estrutura')
                registro = dict(zip(colunas, valores))
                identificador = int(registro['id'])
                carcacas += 1
                for nome in re.findall(r'[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\.(?:jpg|jpeg|png)', registro['fotos']):
                    vinculos.setdefault(nome.lower(), set()).add(identificador)
    destino.mkdir(parents=True, exist_ok=True)
    with (destino / 'vinculos_cadastro_nao_revisado.csv').open('w', newline='') as saida:
        tabela = csv.writer(saida)
        tabela.writerow(['arquivo_nome', 'carcaca_ids', 'grupo_candidato', 'estado_vinculo', 'origem', 'elegivel_treino'])
        for nome, identificadores in sorted(vinculos.items()):
            unico = len(identificadores) == 1
            grupo = f'gppremium:carcaca:{next(iter(identificadores))}' if unico else ''
            tabela.writerow([nome, json.dumps(sorted(identificadores)), grupo,
                             'LEGADO_A_CONFERIR' if unico else 'CONFLITANTE', 'LEGADO_NAO_REVISADO', False])
    checksum = hashlib.sha256()
    with dump.open('rb') as entrada:
        for bloco in iter(lambda: entrada.read(1024 * 1024), b''):
            checksum.update(bloco)
    resumo = {'dump': dump.name, 'dump_sha256': checksum.hexdigest(), 'carcacas_no_dump': carcacas,
              'nomes_fotos_referenciados': len(vinculos), 'nomes_com_multiplas_carcacas': sum(len(v) > 1 for v in vinculos.values()),
              'rotulos_aprovados': 0, 'observacao': 'Vínculo de cadastro não é transcrição visual. Conferir grupo físico antes de congelar o teste.'}
    (destino / 'resumo_vinculos.json').write_text(json.dumps(resumo, ensure_ascii=False, indent=2) + '\n')
    return resumo


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--dump', type=Path, required=True)
    parser.add_argument('--destino', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(extrair(args.dump, args.destino), ensure_ascii=False))
