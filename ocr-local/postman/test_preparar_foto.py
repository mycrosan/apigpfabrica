import argparse
import contextlib
import io
import unittest
from unittest.mock import Mock

from preparar_foto import ler_foto


class LeituraFotoTest(unittest.TestCase):
    def foto(self):
        foto = Mock()
        foto.is_file.return_value = True
        foto.stat.return_value.st_size = 4
        foto.read_bytes.return_value = b'foto'
        return foto

    def test_preserva_bytes_da_foto(self):
        self.assertEqual(ler_foto(self.foto(), argparse.ArgumentParser()), b'foto')

    def test_permissao_negada_orienta_sem_stack_trace(self):
        for operacao in ['is_file', 'stat', 'read_bytes']:
            with self.subTest(operacao=operacao):
                foto = self.foto()
                getattr(foto, operacao).side_effect = PermissionError(1, 'Operation not permitted')
                saida = io.StringIO()
                with contextlib.redirect_stderr(saida), self.assertRaises(SystemExit) as erro:
                    ler_foto(foto, argparse.ArgumentParser())
                self.assertEqual(erro.exception.code, 2)
                self.assertIn('Arquivos e Pastas', saida.getvalue())
                self.assertNotIn('Traceback', saida.getvalue())

    def test_falha_de_leitura_tem_orientacao_propria(self):
        foto = self.foto()
        foto.read_bytes.side_effect = OSError('Arquivo indisponível')
        saida = io.StringIO()
        with contextlib.redirect_stderr(saida), self.assertRaises(SystemExit):
            ler_foto(foto, argparse.ArgumentParser())
        self.assertIn('disponível localmente', saida.getvalue())


if __name__ == '__main__':
    unittest.main()
