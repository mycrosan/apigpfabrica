from threading import Lock
from unittest.mock import Mock

import numpy as np
from unittest import TestCase
from PIL import Image

from pneus_ocr.engine import MotorLocal, PERFIL_DOT


def motor_teste(perfil=PERFIL_DOT):
    motor = MotorLocal.__new__(MotorLocal)
    motor.perfil_dot = perfil
    motor._lock = Lock()
    motor._modelo = Mock()
    return motor


def resultado(texto):
    return [{'rec_texts': [texto], 'rec_scores': [0.9],
             'rec_polys': [np.array([[1, 2], [10, 2], [10, 8], [1, 8]])]}]


class TestMotorLocal(TestCase):
    def test_dot_preserva_conflitos_e_coordenadas_sem_alterar_original(self):
        motor = motor_teste()
        motor._modelo.predict.side_effect = [resultado('3524'), resultado('3624')]
        imagem = Image.new('RGB', (30, 20), (100, 30, 200))
        original = imagem.tobytes()
        linhas = motor.reconhecer(imagem, 'DOT')
        assert [linha['texto'] for linha in linhas] == ['3524', '3624']
        assert linhas[1]['regiao'] == [[1, 2], [10, 2], [10, 8], [1, 8]]
        assert imagem.tobytes() == original
        cinza = motor._modelo.predict.call_args_list[1].args[0]
        assert cinza.shape == (20, 30, 3)
        assert np.array_equal(cinza[:, :, 0], cinza[:, :, 1])
        assert np.array_equal(cinza[:, :, 1], cinza[:, :, 2])


    def test_outros_campos_e_rollback_usam_uma_passagem(self):
        for campo, perfil in [('MARCA', PERFIL_DOT), ('DOT', 'legado')]:
            motor = motor_teste(perfil)
            motor._modelo.predict.return_value = resultado('TESTE')
            assert len(motor.reconhecer(Image.new('RGB', (30, 20)), campo)) == 1
            assert motor._modelo.predict.call_count == 1
            assert motor.preprocessamento(campo) == 'exif-transpose-rgb-v1'


    def test_falha_adicional_nao_vira_sucesso_parcial_e_libera_motor(self):
        motor = motor_teste()
        motor._modelo.predict.side_effect = [resultado('3524'), RuntimeError('Falha local')]
        with self.assertRaises(RuntimeError):
            motor.reconhecer(Image.new('RGB', (30, 20)), 'DOT')
        assert not motor._lock.locked()
