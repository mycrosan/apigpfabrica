"""Baseline sobre rótulos aprovados do gabarito. N pequeno não sustenta intervalo de confiança."""
import argparse
import base64
import json
import os
import statistics
import time
import urllib.error
import urllib.request
from pathlib import Path

OCR_URL = os.environ.get('OCR_URL', 'http://127.0.0.1:8091')
OCR_TOKEN = os.environ.get('OCR_TOKEN', '')


def normalizar(texto):
    return ''.join(texto.upper().split())


def distancia_edicao(a, b):
    anterior = list(range(len(b) + 1))
    for i, ca in enumerate(a, 1):
        atual = [i] + [0] * len(b)
        for j, cb in enumerate(b, 1):
            custo = 0 if ca == cb else 1
            atual[j] = min(anterior[j] + 1, atual[j - 1] + 1, anterior[j - 1] + custo)
        anterior = atual
    return anterior[-1]


def reconhecer(campo, foto_base64):
    corpo = json.dumps({'campo': campo, 'foto_base64': foto_base64}).encode()
    requisicao = urllib.request.Request(OCR_URL + '/v1/reconhecer', data=corpo,
        headers={'Content-Type': 'application/json', 'Authorization': 'Bearer ' + OCR_TOKEN})
    try:
        with urllib.request.urlopen(requisicao, timeout=60) as resposta:
            return resposta.status, json.load(resposta)
    except urllib.error.HTTPError as erro:
        return erro.code, json.load(erro)


def avaliar_registro(raiz, registro):
    campo = registro['campo']
    esperado = normalizar(registro['transcricao_literal'])
    saida = {'imagem_sha256': registro['imagem_sha256'], 'arquivo': registro['arquivo'], 'campo': campo,
              'legibilidade': registro['legibilidade'], 'transcricao_literal': registro['transcricao_literal']}
    if registro['legibilidade'] != 'LEGIVEL':
        saida['avaliado'] = False
        saida['observacao'] = 'Fora do escopo desta métrica: gabarito não marca a imagem como legível.'
        return saida
    caminho = raiz / registro['arquivo']
    foto_base64 = base64.b64encode(caminho.read_bytes()).decode()
    inicio = time.monotonic()
    status, resposta = reconhecer(campo, foto_base64)
    saida['duracao_ms'] = round((time.monotonic() - inicio) * 1000, 2)
    saida['http_status'] = status
    if status != 200:
        saida['avaliado'] = True
        saida['acerto_exato'] = False
        saida['cer'] = 1.0
        saida['linhas'] = []
        return saida
    linhas = resposta.get('linhas', [])
    saida['linhas'] = [{'texto': l['texto'], 'escore': l['escore']} for l in linhas]
    saida['versao_modelo'] = resposta.get('versaoModelo')
    saida['versao_preprocessamento'] = resposta.get('versaoPreprocessamento')
    if not linhas:
        saida['avaliado'] = True
        saida['acerto_exato'] = False
        saida['cer'] = 1.0
        return saida
    melhor_cer = min(distancia_edicao(normalizar(l['texto']), esperado) / max(1, len(esperado)) for l in linhas)
    saida['avaliado'] = True
    saida['acerto_exato'] = any(normalizar(l['texto']) == esperado for l in linhas)
    saida['cer'] = round(melhor_cer, 4)
    return saida


def executar(raiz, gabarito, saida_arquivo):
    registros = [json.loads(linha) for linha in gabarito.read_text().splitlines() if linha.strip()]
    avaliacoes = [avaliar_registro(raiz, registro) for registro in registros]
    por_campo = {}
    for avaliacao in avaliacoes:
        campo = avaliacao['campo']
        por_campo.setdefault(campo, []).append(avaliacao)
    resumo_campos = {}
    for campo, itens in sorted(por_campo.items()):
        avaliados = [i for i in itens if i['avaliado']]
        resumo_campos[campo] = {
            'n_gabarito': len(itens),
            'n_avaliado': len(avaliados),
            'acertos_exatos': sum(i['acerto_exato'] for i in avaliados),
            'cer_medio': round(statistics.mean(i['cer'] for i in avaliados), 4) if avaliados else None,
            'cobertura': round(sum(bool(i['linhas']) for i in avaliados) / len(avaliados), 4) if avaliados else None,
        }
    resultado = {
        'gerado_em': time.strftime('%Y-%m-%dT%H:%M:%S%z'),
        'gabarito_fonte': str(gabarito),
        'total_registros': len(registros),
        'por_campo': resumo_campos,
        'avaliacoes': avaliacoes,
        'aviso': ('N por campo é de unidades, não de centenas ou milhares. Nenhum número aqui sustenta '
                  'intervalo de confiança nem cumpre o critério de aceite da spec (issue #9). Serve apenas '
                  'para não iniciar a medição de baseline do zero quando houver mais rótulos aprovados.'),
    }
    saida_arquivo.write_text(json.dumps(resultado, ensure_ascii=False, indent=2) + '\n')
    return resultado


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--raiz', type=Path, required=True, help='Raiz do repositório fabrica/, para resolver os caminhos do gabarito')
    parser.add_argument('--gabarito', type=Path, required=True)
    parser.add_argument('--saida', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(executar(args.raiz, args.gabarito, args.saida)['por_campo'], ensure_ascii=False, indent=2))
