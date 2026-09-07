"""Invariantes de dados: duplicata não vira amostra adicional e legado não vira rótulo."""
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


inventario = carregar('inventariar_fotos')
vinculos = carregar('vincular_cadastros_legados')
corpus = carregar('consolidar_corpus')


class PreparacaoDadosTest(unittest.TestCase):
    def test_deduplica_sem_aprovar_rotulos_e_preserva_originais(self):
        with tempfile.TemporaryDirectory() as temporario:
            raiz = Path(temporario) / 'fotos'
            raiz.mkdir()
            Image.new('RGB', (16, 16), 'white').save(raiz / 'a.jpg')
            original = (raiz / 'a.jpg').read_bytes()
            (raiz / 'b.jpg').write_bytes(original)
            (raiz / 'corrompida.jpg').write_bytes(b'nao e imagem')
            destino = Path(temporario) / 'saida'
            resumo = inventario.inventariar(raiz, destino, trabalhadores=1)
            self.assertEqual(resumo['copias_exatas'], 1)
            self.assertEqual(resumo['decodificaveis_unicas'], 1)
            self.assertEqual(resumo['estados']['INVALIDA'], 1)
            self.assertEqual(resumo['rotulos_aprovados'], 0)
            registros = [json.loads(linha) for linha in (destino / 'manifesto.jsonl').read_text().splitlines()]
            self.assertTrue(all(not item['elegivel_treino'] and item['grupo_fisico'] is None for item in registros))
            self.assertEqual((raiz / 'a.jpg').read_bytes(), original)

    def test_nao_segue_foto_fora_da_origem(self):
        with tempfile.TemporaryDirectory() as temporario:
            raiz = Path(temporario) / 'fotos'
            raiz.mkdir()
            externo = Path(temporario) / 'externa.jpg'
            externo.write_bytes(b'fora')
            atalho = raiz / 'atalho.jpg'
            atalho.symlink_to(externo)
            registro = inventario.examinar((raiz, atalho))
            self.assertEqual(registro['motivo'], 'LINK_OU_CAMINHO_EXTERNO')
            self.assertIsNone(registro['sha256'])

    def test_parser_preserva_virgulas_parenteses_e_aspas_escapadas(self):
        sql = r"(1,'texto, (exemplo) e \'aspas\'','[\"foto.jpg\"]'),(2,NULL,'[]');"
        linhas = list(vinculos.linhas_insert(sql))
        self.assertEqual(len(linhas), 2)
        self.assertEqual(len(linhas[0]), 3)
        self.assertEqual(linhas[1], ['2', 'NULL', "'[]'"])

    def test_parser_rejeita_insert_incompleto(self):
        with self.assertRaises(ValueError):
            list(vinculos.linhas_insert("(1,'texto incompleto"))

    def test_mesma_imagem_em_cadastros_distintos_fica_em_conflito(self):
        import csv
        with tempfile.TemporaryDirectory() as temporario:
            raiz = Path(temporario) / 'fotos'
            raiz.mkdir()
            Image.new('RGB', (16, 16), 'white').save(raiz / 'a.jpg')
            (raiz / 'b.jpg').write_bytes((raiz / 'a.jpg').read_bytes())
            destino = Path(temporario) / 'saida'
            inventario.inventariar(raiz, destino, 1)
            with (destino / 'vinculos_cadastro_nao_revisado.csv').open('w', newline='') as arquivo:
                tabela = csv.writer(arquivo)
                tabela.writerow(['arquivo_nome', 'carcaca_ids'])
                tabela.writerow(['a.jpg', '[1]'])
                tabela.writerow(['b.jpg', '[2]'])
            resumo = corpus.consolidar(destino)
            self.assertEqual(resumo['estados_grupo'], {'CONFLITANTE': 1})
            self.assertFalse(resumo['treino_iniciado'])
            registro = json.loads((destino / 'corpus_nao_revisado.jsonl').read_text())
            self.assertEqual(registro['arquivos'], ['a.jpg', 'b.jpg'])
            self.assertFalse(registro['elegivel_treino'])
            self.assertIsNone(registro['particao'])


if __name__ == '__main__':
    unittest.main()
