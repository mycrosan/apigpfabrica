"""Download explícito de pesos públicos; nunca importado pela API de runtime."""
import hashlib
import io
import json
import tarfile
import urllib.request
from pathlib import Path

DESTINO = Path('models')
BASE = 'https://paddle-model-ecology.bj.bcebos.com/paddlex/official_inference_model/paddle3.0.0/'
MODELOS = ('PP-OCRv5_mobile_det', 'PP-OCRv5_mobile_rec')
DESTINO.mkdir(exist_ok=True)
origens = {}
for nome in MODELOS:
    url = BASE + nome + '_infer.tar'
    with urllib.request.urlopen(url, timeout=120) as resposta:
        dados = resposta.read()
    origens[nome] = {'url': url, 'sha256': hashlib.sha256(dados).hexdigest()}
    with tarfile.open(fileobj=io.BytesIO(dados)) as pacote:
        for membro in pacote.getmembers():
            if membro.issym() or membro.islnk() or not (membro.isfile() or membro.isdir()):
                raise ValueError('Arquivo de modelo contém entrada não permitida')
            if not (DESTINO / membro.name).resolve().is_relative_to(DESTINO.resolve()):
                raise ValueError('Arquivo de modelo fora do diretório esperado')
        pacote.extractall(DESTINO, filter='data')
    pasta = DESTINO / (nome + '_infer')
    if pasta.exists():
        pasta.rename(DESTINO / nome)
files = {str(p.relative_to(DESTINO)): hashlib.sha256(p.read_bytes()).hexdigest()
         for p in sorted(DESTINO.rglob('*')) if p.is_file() and p.name != 'manifest.json'}
manifesto = {'detector': MODELOS[0], 'reconhecedor': MODELOS[1], 'origens': origens, 'files': files}
(DESTINO / 'manifest.json').write_text(json.dumps(manifesto, ensure_ascii=False, sort_keys=True, indent=2))
print('Pesos provisionados. Preserve o manifesto junto aos artefatos versionados.')
