"""Registra e verifica pesos já baixados; não baixa modelos nem envia imagens."""
import argparse
import hashlib
import json
import re
import urllib.request
from datetime import datetime, timezone
from pathlib import Path


class SemRedirecionamento(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise ValueError('Redirecionamento do Ollama não permitido')


def consultar(rota, corpo=None):
    cliente = urllib.request.build_opener(urllib.request.ProxyHandler({}), SemRedirecionamento())
    dados = None if corpo is None else json.dumps(corpo).encode()
    requisicao = urllib.request.Request('http://127.0.0.1:11434' + rota, data=dados,
                                      headers={'Content-Type': 'application/json'})
    with cliente.open(requisicao, timeout=10) as resposta:
        return json.load(resposta)


def checksum(arquivo):
    digest = hashlib.sha256()
    with arquivo.open('rb') as entrada:
        for bloco in iter(lambda: entrada.read(1024 * 1024), b''):
            digest.update(bloco)
    return digest.hexdigest()


def registrar(modelo, diretorio, destino):
    if not re.fullmatch(r'[a-z0-9_.-]+:[a-z0-9_.-]+', modelo) or 'cloud' in modelo:
        raise ValueError('Use um modelo local explícito no formato nome:tag')
    itens = consultar('/api/tags')['models']
    item = next((item for item in itens if item['name'] == modelo), None)
    if item is None:
        raise ValueError('Modelo não provisionado; baixe os pesos antes de registrar')
    detalhes = consultar('/api/show', {'model': modelo})
    if 'vision' not in detalhes.get('capabilities', []):
        raise ValueError('Modelo não oferece visão')
    if any(origem.get(chave) for origem in (item, detalhes)
           for chave in ('remote_host', 'remote_model')):
        raise ValueError('Modelo remoto não permitido')
    nome, tag = modelo.split(':')
    arquivo = diretorio / 'manifests' / 'registry.ollama.ai' / 'library' / nome / tag
    digest = checksum(arquivo)
    if digest != item['digest'].removeprefix('sha256:'):
        raise ValueError('Manifesto do disco diverge do servidor')
    manifesto_pesos = json.loads(arquivo.read_text())
    arquivos = {}
    for camada in [manifesto_pesos['config'], *manifesto_pesos['layers']]:
        esperado = camada['digest']
        if not re.fullmatch(r'sha256:[0-9a-f]{64}', esperado):
            raise ValueError('Digest de camada inválido')
        blob = diretorio / 'blobs' / esperado.replace(':', '-')
        if checksum(blob) != esperado.removeprefix('sha256:'):
            raise ValueError('Checksum de peso inválido')
        arquivos[blob.name] = esperado
    manifesto = {'modelo': modelo, 'digest': digest,
                 'versaoOllama': consultar('/api/version')['version'],
                 'arquivos': arquivos, 'detalhes': item.get('details', {}),
                 'geradoEm': datetime.now(timezone.utc).isoformat()}
    destino.parent.mkdir(parents=True, exist_ok=True)
    # Não substitui silenciosamente uma versão já registrada para avaliação.
    with destino.open('x') as saida:
        json.dump(manifesto, saida, indent=2, ensure_ascii=False)
        saida.write('\n')
    print(f'Manifesto verificado: {destino} ({len(arquivos)} arquivos)')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--modelo', default='gemma3:4b')
    parser.add_argument('--pesos', type=Path, default=Path.home() / '.ollama/models')
    parser.add_argument('--saida', type=Path, default=Path('ollama-local/manifest.json'))
    argumentos = parser.parse_args()
    registrar(argumentos.modelo, argumentos.pesos, argumentos.saida)
