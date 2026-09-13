from threading import Lock
from unittest.mock import Mock

import numpy as np
from unittest import TestCase
from PIL import Image

from pneus_ocr.engine import (MotorLocal, PERFIL_DOT, PERFIL_RELEVO, PERFIL_MORFOLOGICO,
                               PERFIL_ROTACAO, RECONHECEDOR_MOBILE, RECONHECEDOR_SERVIDOR,
                               LADO_MAXIMO_SERVIDOR)


def motor_teste(perfil=PERFIL_DOT, perfil_relevo=PERFIL_RELEVO, perfil_morfologico=PERFIL_MORFOLOGICO,
                 perfil_rotacao='legado', reconhecedor_relevo=RECONHECEDOR_MOBILE):
    motor = MotorLocal.__new__(MotorLocal)
    motor.perfil_dot = perfil
    motor.perfil_relevo = perfil_relevo
    motor.perfil_morfologico = perfil_morfologico
    motor.perfil_rotacao = perfil_rotacao
    motor.reconhecedor_relevo = reconhecedor_relevo
    motor._lock = Lock()
    motor._modelo = Mock()
    motor._modelo_relevo = Mock() if reconhecedor_relevo == RECONHECEDOR_SERVIDOR else None
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


    def test_campos_em_relevo_ganham_tres_passagens_original_cinza_e_morfologica(self):
        # Foto real de medida: sem a passagem morfologica o detector so achava um fragmento
        # ('70R'); com ela, a linha inteira ('175/70R14C'). Rotacao fica fora por padrao (ver
        # CAMPOS_ROTACAO em engine.py), entao MEDIDA/PAIS se comportam igual a MARCA/MODELO aqui.
        for campo in ('MARCA', 'MODELO', 'MEDIDA', 'PAIS'):
            motor = motor_teste()
            motor._modelo.predict.side_effect = [
                resultado('X'), resultado('MADEINCHINA'), resultado('175/70R14C')]
            linhas = motor.reconhecer(Image.new('RGB', (30, 20)), campo)
            assert [linha['texto'] for linha in linhas] == ['X', 'MADEINCHINA', '175/70R14C']
            assert motor._modelo.predict.call_count == 3
            assert motor.preprocessamento(campo) == 'exif-transpose-rgb-relevo-v1+morfologico-v1'

    def test_rotacao_e_opt_in_e_repete_as_tres_passagens_em_cada_angulo(self):
        # Foto real da carcaça 202605 (13/09/2026): nenhuma das quatro fotos chegou ao motor ja
        # com o texto na horizontal. Rotacionar recuperou leitura exata em MEDIDA (0,78 a 180°) e
        # melhorou o escore em PAIS (0,89 a 180°) -- mas uma chamada real com isso ligado mediu
        # 29,6s, quase 3x o teto de 10s p95 da spec. Por isso fica desligado por padrao (teste
        # acima) e só liga explicitamente aqui, documentando o custo: 4x a pilha de passagens.
        for campo in ('MEDIDA', 'PAIS'):
            motor = motor_teste(perfil_rotacao=PERFIL_ROTACAO)
            motor._modelo.predict.side_effect = [resultado(str(indice)) for indice in range(12)]
            linhas = motor.reconhecer(Image.new('RGB', (30, 20)), campo)
            assert len(linhas) == 12
            assert motor._modelo.predict.call_count == 12
            assert motor.preprocessamento(campo) == 'exif-transpose-rgb-relevo-v1+morfologico-v1+rotacao-v1'

    def test_passagem_morfologica_nao_afeta_o_dot(self):
        # O DOT ja esta em producao com sua propria passagem ajustada; a passagem morfologica
        # e so para o alfabeto grande vazado de marca/modelo/medida/pais.
        motor = motor_teste()
        motor._modelo.predict.side_effect = [resultado('3524'), resultado('3624')]
        linhas = motor.reconhecer(Image.new('RGB', (30, 20)), 'DOT')
        assert len(linhas) == 2
        assert motor._modelo.predict.call_count == 2
        assert motor.preprocessamento('DOT') == 'exif-transpose-rgb-' + PERFIL_DOT

    def test_rollback_do_morfologico_e_independente_do_relevo(self):
        # Desligar so o morfologico preserva a passagem em cinza (ja validada) e some so com a
        # terceira passagem -- rollback fino, sem voltar ao comportamento de uma passagem so.
        motor = motor_teste(perfil_morfologico='legado')
        motor._modelo.predict.side_effect = [resultado('X'), resultado('MADEINCHINA')]
        linhas = motor.reconhecer(Image.new('RGB', (30, 20)), 'PAIS')
        assert [linha['texto'] for linha in linhas] == ['X', 'MADEINCHINA']
        assert motor._modelo.predict.call_count == 2
        assert motor.preprocessamento('PAIS') == 'exif-transpose-rgb-relevo-v1'

    def test_rollback_por_campo_e_independente(self):
        # Desligar o relevo (e o morfologico junto, ja que depende dele) dos demais campos nao
        # pode afetar o DOT, que ja esta em producao.
        motor = motor_teste(perfil_relevo='legado')
        motor._modelo.predict.return_value = resultado('TESTE')
        assert len(motor.reconhecer(Image.new('RGB', (30, 20)), 'MARCA')) == 1
        assert motor.preprocessamento('MARCA') == 'exif-transpose-rgb-v1'
        assert motor.preprocessamento('DOT') == 'exif-transpose-rgb-' + PERFIL_DOT

        somente_relevo = motor_teste(perfil='legado')
        somente_relevo._modelo.predict.return_value = resultado('TESTE')
        assert len(somente_relevo.reconhecer(Image.new('RGB', (30, 20)), 'DOT')) == 1
        assert somente_relevo.preprocessamento('DOT') == 'exif-transpose-rgb-v1'


    # A promessa do modo "servidor" é ler o que o mobile nunca leu (HIFLY, 175/70R14C em foto
    # real) -- mas custa memória, então ele SUBSTITUI as passagens em mobile nos campos de
    # relevo em vez de se somar a elas: uma chamada, não três, e a mobile nunca roda à toa.
    def test_reconhecedor_servidor_substitui_o_mobile_em_marca_modelo_e_medida(self):
        for campo in ('MARCA', 'MODELO', 'MEDIDA'):
            motor = motor_teste(reconhecedor_relevo=RECONHECEDOR_SERVIDOR)
            motor._modelo_relevo.predict.return_value = resultado('HIFLY')
            linhas = motor.reconhecer(Image.new('RGB', (30, 20)), campo)
            assert [linha['texto'] for linha in linhas] == ['HIFLY']
            motor._modelo_relevo.predict.assert_called_once()
            motor._modelo.predict.assert_not_called()
            assert motor.preprocessamento(campo) == 'exif-transpose-rgb-relevo-servidor-v1'

    # PAIS fica de fora por escolha (comentário de CAMPOS_SERVIDOR_ELEGIVEL em engine.py):
    # medido em foto real, o mobile já acerta o país e o servidor, na mesma foto, errou uma
    # letra. Ligar "servidor" não pode regredir um campo que já funciona.
    def test_reconhecedor_servidor_nao_se_aplica_a_pais(self):
        motor = motor_teste(reconhecedor_relevo=RECONHECEDOR_SERVIDOR)
        motor._modelo.predict.side_effect = [resultado('X'), resultado('MADEINCHINA'),
                                              resultado('sobra')]
        linhas = motor.reconhecer(Image.new('RGB', (30, 20)), 'PAIS')
        assert 'MADEINCHINA' in [linha['texto'] for linha in linhas]
        motor._modelo_relevo.predict.assert_not_called()
        assert motor.preprocessamento('PAIS') == 'exif-transpose-rgb-relevo-v1+morfologico-v1'

    def test_reconhecedor_servidor_nao_afeta_o_dot(self):
        motor = motor_teste(reconhecedor_relevo=RECONHECEDOR_SERVIDOR)
        motor._modelo.predict.side_effect = [resultado('3524'), resultado('3624')]
        linhas = motor.reconhecer(Image.new('RGB', (30, 20)), 'DOT')
        assert [linha['texto'] for linha in linhas] == ['3524', '3624']
        motor._modelo_relevo.predict.assert_not_called()
        assert motor.preprocessamento('DOT') == 'exif-transpose-rgb-' + PERFIL_DOT

    # Em CPU com pouca memória, decodificar e copiar a foto em resolução total antes de
    # reduzir já é o bastante para faltar memória -- reduzir tem que acontecer ANTES do Paddle,
    # não só via parâmetro de detecção.
    def test_reconhecedor_servidor_reduz_imagem_grande_antes_de_enviar(self):
        motor = motor_teste(reconhecedor_relevo=RECONHECEDOR_SERVIDOR)
        motor._modelo_relevo.predict.return_value = resultado('X')
        imagem_grande = Image.new('RGB', (4080, 3072))
        motor.reconhecer(imagem_grande, 'MARCA')
        enviada = motor._modelo_relevo.predict.call_args.args[0]
        assert max(enviada.shape[:2]) == LADO_MAXIMO_SERVIDOR

    def test_reconhecedor_servidor_nao_reduz_imagem_pequena(self):
        motor = motor_teste(reconhecedor_relevo=RECONHECEDOR_SERVIDOR)
        motor._modelo_relevo.predict.return_value = resultado('X')
        motor.reconhecer(Image.new('RGB', (400, 300)), 'MARCA')
        enviada = motor._modelo_relevo.predict.call_args.args[0]
        assert enviada.shape[:2] == (300, 400)

    def test_falha_adicional_nao_vira_sucesso_parcial_e_libera_motor(self):
        motor = motor_teste()
        motor._modelo.predict.side_effect = [resultado('3524'), RuntimeError('Falha local')]
        with self.assertRaises(RuntimeError):
            motor.reconhecer(Image.new('RGB', (30, 20)), 'DOT')
        assert not motor._lock.locked()
