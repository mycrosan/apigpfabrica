"""Invariantes do recorte: orientação correta, original intacto e revisão ainda cega."""
import csv
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

from PIL import Image


def carregar(nome):
    caminho = Path(__file__).parents[1] / 'scripts' / (nome + '.py')
    spec = importlib.util.spec_from_file_location(nome, caminho)
    modulo = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(modulo)
    return modulo


recorte = carregar('recortar_regioes_revisao')

CABECALHO = ['imagem_sha256', 'arquivo', 'regiao_indice', 'regiao_json', 'campo',
             'transcricao_humana', 'legibilidade', 'grupo_fisico', 'revisor_id',
             'revisado_em', 'estado_revisao']


def planilha(caminho, linhas):
    with Path(caminho).open('w', encoding='utf-8', newline='') as arquivo:
        escritor = csv.DictWriter(arquivo, fieldnames=CABECALHO)
        escritor.writeheader()
        escritor.writerows(linhas)


def linha(sha, arquivo, indice, regiao):
    dados = {campo: '' for campo in CABECALHO}
    dados.update(imagem_sha256=sha, arquivo=arquivo, regiao_indice=str(indice),
                 regiao_json=json.dumps(regiao), estado_revisao='PENDENTE')
    return dados


class RecorteRevisaoTest(unittest.TestCase):
    def test_aplica_orientacao_exif_e_preserva_original(self):
        with tempfile.TemporaryDirectory() as temporario:
            raiz = Path(temporario)
            fotos = raiz / 'fotos'
            fotos.mkdir()
            # Deitada no arquivo (200x100) mas com EXIF 6: o serviço enxerga 100x200.
            imagem = Image.new('RGB', (200, 100), 'white')
            exif = imagem.getexif()
            exif[274] = 6
            caminho = fotos / 'a.jpg'
            imagem.save(caminho, exif=exif)
            antes = caminho.read_bytes()
            csv_entrada = raiz / 'cega.csv'
            # Região válida somente no espaço rotacionado: y=150 excede a altura crua de 100.
            planilha(csv_entrada, [linha('a' * 64, 'a.jpg', 0, [[10, 140], [80, 140], [80, 170], [10, 170]])])
            contagem = recorte.recortar(csv_entrada, fotos, raiz / 'saida')
            self.assertEqual(contagem[recorte.FALHA_RECORTE], 0)
            self.assertEqual(contagem[recorte.REVISAR], 1)
            self.assertEqual(caminho.read_bytes(), antes)

    def test_regiao_nula_nao_quebra_e_nao_vira_recorte(self):
        with tempfile.TemporaryDirectory() as temporario:
            raiz = Path(temporario)
            fotos = raiz / 'fotos'
            fotos.mkdir()
            Image.new('RGB', (60, 60), 'white').save(fotos / 'b.jpg')
            csv_entrada = raiz / 'cega.csv'
            dados = {campo: '' for campo in CABECALHO}
            dados.update(imagem_sha256='b' * 64, arquivo='b.jpg', regiao_indice='0',
                         regiao_json='null', estado_revisao='PENDENTE')
            planilha(csv_entrada, [dados])
            contagem = recorte.recortar(csv_entrada, fotos, raiz / 'saida')
            self.assertEqual(contagem[recorte.SEM_REGIAO], 1)
            self.assertFalse(any((raiz / 'saida' / 'recortes').iterdir()))

    def test_triagem_usa_a_caixa_proposta_e_nao_a_margem(self):
        with tempfile.TemporaryDirectory() as temporario:
            raiz = Path(temporario)
            fotos = raiz / 'fotos'
            fotos.mkdir()
            Image.new('RGB', (400, 400), 'white').save(fotos / 'c.jpg')
            csv_entrada = raiz / 'cega.csv'
            planilha(csv_entrada, [linha('c' * 64, 'c.jpg', 0, [[100, 100], [112, 100], [112, 110], [100, 110]])])
            contagem = recorte.recortar(csv_entrada, fotos, raiz / 'saida')
            # A caixa tem 10 px de altura; a margem mínima de 8 px não pode promovê-la a REVISAR.
            self.assertEqual(contagem[recorte.RUIDO_PROVAVEL], 1)
            self.assertEqual(contagem[recorte.REVISAR], 0)

    def test_planilha_de_saida_nao_expoe_transcricao_do_ocr(self):
        with tempfile.TemporaryDirectory() as temporario:
            raiz = Path(temporario)
            fotos = raiz / 'fotos'
            fotos.mkdir()
            Image.new('RGB', (400, 400), 'white').save(fotos / 'd.jpg')
            csv_entrada = raiz / 'cega.csv'
            planilha(csv_entrada, [linha('d' * 64, 'd.jpg', 0, [[10, 10], [200, 10], [200, 90], [10, 90]])])
            recorte.recortar(csv_entrada, fotos, raiz / 'saida')
            with (raiz / 'saida' / 'revisao_cega_com_recortes.csv').open(encoding='utf-8') as arquivo:
                saida = list(csv.DictReader(arquivo))
            self.assertEqual(saida[0]['transcricao_humana'], '')
            self.assertEqual(saida[0]['estado_revisao'], 'PENDENTE')
            self.assertNotIn('texto', ','.join(saida[0].keys()))


if __name__ == '__main__':
    unittest.main()
