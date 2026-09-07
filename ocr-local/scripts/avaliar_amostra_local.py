"""Teste de operação em fotos reais. Não mede acurácia nem produz rótulos aprovados."""
import base64
import csv
import hashlib
import json
import os
from pathlib import Path
import secrets
import statistics
import subprocess
import sys
import time
import urllib.error
import urllib.request


def solicitar(caminho, dados=None):
    corpo = None if dados is None else json.dumps(dados).encode()
    requisicao = urllib.request.Request('http://127.0.0.1:8091' + caminho, data=corpo,
        headers={'Content-Type': 'application/json', 'Authorization': 'Bearer ' + os.environ['OCR_TOKEN']})
    try:
        with urllib.request.urlopen(requisicao, timeout=60) as resposta:
            return resposta.status, json.load(resposta)
    except urllib.error.HTTPError as erro:
        return erro.code, json.load(erro)


def main():
    origem = Path('/fotos')
    destino = Path('/saida')
    destino.mkdir(parents=True, exist_ok=True)
    os.environ['OCR_TOKEN'] = secrets.token_urlsafe(32)
    servidor = subprocess.Popen([sys.executable, '-m', 'uvicorn', 'pneus_ocr.app:app', '--host', '127.0.0.1', '--port', '8091'],
                                stdout=(destino / 'servico.log').open('w'), stderr=subprocess.STDOUT)
    inicio = time.monotonic()
    try:
        pronto = None
        for _ in range(90):
            try:
                estado, pronto = solicitar('/ready')
                if estado == 200:
                    break
            except (OSError, urllib.error.URLError):
                pass  # Serviço ainda está iniciando; o limite abaixo transforma a espera em falha explícita.
            time.sleep(1)
        else:
            raise RuntimeError('PaddleOCR não ficou pronto em 90 segundos')
        subida = time.monotonic() - inicio
        pastas = sorted(p for p in origem.glob('*/carcaca') if p.is_dir())
        resultados = []
        with (destino / 'leituras_nao_revisadas.jsonl').open('w') as saida, (destino / 'revisao_cega.csv').open('w', newline='') as revisao:
            colunas = ['imagem_sha256', 'arquivo', 'regiao_indice', 'regiao_json', 'campo', 'transcricao_humana',
                       'legibilidade', 'grupo_fisico', 'revisor_id', 'revisado_em', 'estado_revisao']
            tabela = csv.DictWriter(revisao, fieldnames=colunas)
            tabela.writeheader()
            vistos = set()
            for pasta in pastas:
                arquivos = sorted(pasta.glob('*.jpg'), key=lambda p: hashlib.sha256(('20260906:' + p.name).encode()).hexdigest())
                for arquivo in arquivos:
                    dados = arquivo.read_bytes()
                    checksum = hashlib.sha256(dados).hexdigest()
                    if checksum not in vistos:
                        vistos.add(checksum)
                        break
                else:
                    continue
                partida = time.monotonic()
                # O campo não muda a extração do serviço; nenhuma foto recebe rótulo de MARCA por isso.
                codigo, resposta = solicitar('/v1/reconhecer', {'campo': 'MARCA', 'foto_base64': base64.b64encode(dados).decode()})
                duracao = (time.monotonic() - partida) * 1000
                relativo = str(arquivo.relative_to(origem))
                registro = {'arquivo': relativo, 'imagem_sha256': checksum, 'http_status': codigo,
                            'duracao_total_ms': round(duracao, 2), 'origem': 'LEGADO_NAO_REVISADO',
                            'campo_rotulado': None, 'elegivel_treino': False, 'resposta_ocr_nao_revisada': resposta}
                saida.write(json.dumps(registro, ensure_ascii=False) + '\n')
                saida.flush()
                linhas = resposta.get('linhas', [])
                for indice, linha in enumerate(linhas or [{}]):
                    tabela.writerow({'imagem_sha256': checksum, 'arquivo': relativo, 'regiao_indice': indice,
                                     'regiao_json': json.dumps(linha.get('regiao')), 'estado_revisao': 'PENDENTE'})
                resultados.append({'http_status': codigo, 'duracao_ms': duracao, 'regioes': len(linhas)})
                print(json.dumps({'fotos_processadas': len(resultados), 'total_pastas': len(pastas),
                                  'http_status': codigo, 'regioes': len(linhas)}), flush=True)
        tempos = sorted(r['duracao_ms'] for r in resultados)
        resumo = {'fotos': len(resultados), 'sucessos_http': sum(r['http_status'] == 200 for r in resultados),
                  'com_texto_extraido': sum(r['regioes'] > 0 for r in resultados),
                  'regioes_extraidas': sum(r['regioes'] for r in resultados),
                  'latencia_p50_ms': round(statistics.median(tempos), 2),
                  'latencia_p95_ms': round(tempos[max(0, int(len(tempos) * .95 + .999) - 1)], 2),
                  'subida_segundos': round(subida, 2), 'readiness': pronto,
                  'selecao': 'Uma foto por pasta de origem; ordem determinística pelo hash do nome e semente 20260906; cópias exatas ignoradas.',
                  'rede': 'Contêiner executado com --network none', 'rotulos_aprovados': 0,
                  'precisao': None, 'observacao': 'Resposta 200/texto extraído não comprova leitura correta. Acurácia depende de revisão humana.'}
        (destino / 'resumo.json').write_text(json.dumps(resumo, ensure_ascii=False, indent=2) + '\n')
        print(json.dumps(resumo, ensure_ascii=False), flush=True)
    finally:
        servidor.terminate()
        try:
            servidor.wait(timeout=10)
        except subprocess.TimeoutExpired:
            servidor.kill()
            servidor.wait()


if __name__ == '__main__':
    main()
