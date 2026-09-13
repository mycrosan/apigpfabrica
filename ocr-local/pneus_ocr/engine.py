"""Carregamento de pesos locais e extração sem interpretação de catálogo."""
import hashlib
import json
import os
from pathlib import Path
from threading import Lock

PERFIL_DOT = 'dot-relevo-v1'
PERFIL_RELEVO = 'relevo-v1'
# Limiares de detecção afrouxados. Texto gravado na borracha tem contraste baixo e, nos limiares
# padrão do modelo, o detector devolve caractere solto ou nada. Medido em foto real de país tirada
# na fábrica: o perfil padrão devolveu 'C' (escore 0,16); com esta passagem, 'MADEINCHINA' (0,90).
PARAMETROS_RELEVO = {
    'text_det_limit_side_len': 960,
    'text_det_limit_type': 'max',
    'text_det_thresh': 0.15,
    'text_det_box_thresh': 0.3,
    'text_det_unclip_ratio': 2.0,
}
# Campos cujo valor é gravado ou em relevo no flanco. O DOT mantém chave de rollback própria por
# já estar em produção; os demais entram sob OCR_PERFIL_RELEVO, desligável sem afetar o DOT.
CAMPOS_RELEVO = ('MARCA', 'MODELO', 'MEDIDA', 'PAIS')
# Subconjunto elegível para o reconhecedor "servidor". PAIS fica de fora por escolha, não por
# limitação técnica: medido em foto real, o mobile já lê o país corretamente ("MADEINCHINA",
# 0,90) e o servidor, na mesma foto, trocou uma letra ("MADEINCHNA") -- sem ganho e com risco de
# regressão num campo que já funciona. MARCA/MODELO/MEDIDA nunca leram de forma confiável no
# mobile; é onde o servidor tem o que ganhar.
CAMPOS_SERVIDOR_ELEGIVEL = ('MARCA', 'MODELO', 'MEDIDA')

PERFIL_MORFOLOGICO = 'morfologico-v1'
# Ainda mais frouxo que PARAMETROS_RELEVO: alfabeto grande em relevo vazado (marca/modelo) ocupa
# muito mais área do quadro que o DOT, então precisa de um lado de entrada maior para o detector
# não perder resolução no recorte de cada letra. Medido em foto real: com esses limiares e sem o
# realce morfológico, o detector achava só um fragmento da medida ("70R"); com o realce, achou a
# linha inteira do "175/70R14C".
PARAMETROS_MORFOLOGICO = {
    'text_det_limit_side_len': 1536,
    'text_det_limit_type': 'max',
    'text_det_thresh': 0.10,
    'text_det_box_thresh': 0.20,
    'text_det_unclip_ratio': 2.5,
}


# Pesos maiores (PP-OCRv5 "server"), testados em foto real: leem HIFLY a 0,929 e
# "175/70R14CY" a 0,90 em alfabeto grande vazado que os pesos "mobile" nunca leram, em
# nenhuma das dez variações de pré-processamento tentadas antes disto. A contrapartida é
# memória: em CPU com poucos núcleos disponíveis, mais de 1 thread ou mais de ~1300px de
# lado já derrubou o processo por falta de memória em ambiente com 7-8 GiB de RAM. Por
# isso o modo "servidor" roda de propósito com 1 thread e lado limitado — e continua
# desligado por padrão até o ambiente ter memória confirmada.
RECONHECEDOR_MOBILE = 'mobile'
RECONHECEDOR_SERVIDOR = 'servidor'
LADO_MAXIMO_SERVIDOR = 1280
PARAMETROS_SERVIDOR = {
    'text_det_limit_side_len': LADO_MAXIMO_SERVIDOR,
    'text_det_limit_type': 'max',
    'text_det_thresh': 0.15,
    'text_det_box_thresh': 0.3,
    'text_det_unclip_ratio': 2.0,
}


class MotorLocal:
    def __init__(self, diretorio: Path, diretorio_relevo: Path | None = None):
        self.perfil_dot = os.getenv('OCR_PERFIL_DOT', PERFIL_DOT)
        if self.perfil_dot not in ('legado', PERFIL_DOT):
            raise ValueError('Perfil DOT desconhecido')
        self.perfil_relevo = os.getenv('OCR_PERFIL_RELEVO', PERFIL_RELEVO)
        if self.perfil_relevo not in ('legado', PERFIL_RELEVO):
            raise ValueError('Perfil de relevo desconhecido')
        self.perfil_morfologico = os.getenv('OCR_PERFIL_MORFOLOGICO', PERFIL_MORFOLOGICO)
        if self.perfil_morfologico not in ('legado', PERFIL_MORFOLOGICO):
            raise ValueError('Perfil morfológico desconhecido')
        self.reconhecedor_relevo = os.getenv('OCR_RECONHECEDOR_RELEVO', RECONHECEDOR_MOBILE)
        if self.reconhecedor_relevo not in (RECONHECEDOR_MOBILE, RECONHECEDOR_SERVIDOR):
            raise ValueError('Reconhecedor de relevo desconhecido')
        os.environ['PADDLE_PDX_DISABLE_MODEL_SOURCE_CHECK'] = 'True'
        conteudo, configuracao = self._validar_pesos(diretorio)
        from paddleocr import PaddleOCR
        self._modelo = PaddleOCR(
            text_detection_model_name=configuracao['detector'],
            text_detection_model_dir=str(diretorio / configuracao['detector']),
            text_recognition_model_name=configuracao['reconhecedor'],
            text_recognition_model_dir=str(diretorio / configuracao['reconhecedor']),
            use_doc_orientation_classify=False,
            use_doc_unwarping=False,
            use_textline_orientation=False,
            device='cpu',
            enable_mkldnn=False,
        )
        self._lock = Lock()
        self.versao = hashlib.sha256(conteudo).hexdigest()

        self._modelo_relevo = None
        conteudo_relevo = b''
        if self.reconhecedor_relevo == RECONHECEDOR_SERVIDOR:
            if diretorio_relevo is None:
                raise ValueError('OCR_RECONHECEDOR_RELEVO=servidor exige OCR_MODELOS_RELEVO_DIR')
            conteudo_relevo, configuracao_relevo = self._validar_pesos(diretorio_relevo)
            # Threads e lado de entrada travados no construtor, não expostos por variável:
            # são os únicos valores testados sem faltar memória neste ambiente. Ver comentário
            # de RECONHECEDOR_SERVIDOR acima antes de alterar.
            self._modelo_relevo = PaddleOCR(
                text_detection_model_name=configuracao_relevo['detector'],
                text_detection_model_dir=str(diretorio_relevo / configuracao_relevo['detector']),
                text_recognition_model_name=configuracao_relevo['reconhecedor'],
                text_recognition_model_dir=str(diretorio_relevo / configuracao_relevo['reconhecedor']),
                use_doc_orientation_classify=False,
                use_doc_unwarping=False,
                use_textline_orientation=False,
                device='cpu',
                enable_mkldnn=True,
                cpu_threads=1,
            )

        perfis = {nome: valor for nome, valor in
                  (('DOT', self.perfil_dot), ('RELEVO', self.perfil_relevo),
                   ('MORFOLOGICO', self.perfil_morfologico),
                   ('RECONHECEDOR_RELEVO', self.reconhecedor_relevo)) if valor not in ('legado', RECONHECEDOR_MOBILE)}
        if perfis:
            perfil = json.dumps({'perfis': perfis, 'parametros_relevo': PARAMETROS_RELEVO,
                                 'parametros_morfologico': PARAMETROS_MORFOLOGICO,
                                 'parametros_servidor': PARAMETROS_SERVIDOR,
                                 'conversao': 'opencv-bgr-gray-bgr', 'realce': 'tophat-mais-blackhat',
                                 'estrategia': 'original-mais-cinza-mais-morfologico'},
                                sort_keys=True).encode()
            self.versao = hashlib.sha256(conteudo + conteudo_relevo + perfil).hexdigest()

    # Mesma checagem para os dois diretórios de pesos: hash de cada arquivo contra o
    # manifesto e nenhum caminho escapando do diretório declarado.
    def _validar_pesos(self, diretorio: Path):
        manifesto = diretorio / 'manifest.json'
        conteudo = manifesto.read_bytes()
        configuracao = json.loads(conteudo)
        for nome, checksum in configuracao['files'].items():
            arquivo = (diretorio / nome).resolve()
            if not arquivo.is_relative_to(diretorio.resolve()):
                raise ValueError('Arquivo fora do diretório de modelos')
            if hashlib.sha256(arquivo.read_bytes()).hexdigest() != checksum:
                raise ValueError('Checksum de modelo inválido')
        for nome in (configuracao['detector'], configuracao['reconhecedor']):
            pasta = diretorio / nome
            if not pasta.is_dir() or not (pasta / 'inference.yml').is_file():
                raise ValueError('Pesos locais incompletos')
        return conteudo, configuracao

    def preprocessamento(self, campo):
        if campo == 'DOT' and self.perfil_dot != 'legado':
            return 'exif-transpose-rgb-' + self.perfil_dot
        if campo in CAMPOS_SERVIDOR_ELEGIVEL and self.reconhecedor_relevo == RECONHECEDOR_SERVIDOR:
            return 'exif-transpose-rgb-relevo-servidor-v1'
        if campo in CAMPOS_RELEVO and self.perfil_relevo != 'legado':
            rotulo = 'exif-transpose-rgb-' + self.perfil_relevo
            if self.perfil_morfologico != 'legado':
                rotulo += '+' + self.perfil_morfologico
            return rotulo
        return 'exif-transpose-rgb-v1'

    def reconhecer(self, imagem, campo=None):
        import numpy as np
        if not self._lock.acquire(blocking=False):
            raise BlockingIOError('OCR ocupado')
        try:
            # Paddle espera BGR quando recebe ndarray.
            bgr = np.asarray(imagem)[:, :, ::-1].copy()
            # Reconhecedor "servidor" só substitui o mobile nos campos onde o mobile
            # comprovadamente falha (ver CAMPOS_SERVIDOR_ELEGIVEL) -- nunca no DOT, e nunca no
            # PAIS, que já funciona bem no mobile.
            if campo in CAMPOS_SERVIDOR_ELEGIVEL and self.reconhecedor_relevo == RECONHECEDOR_SERVIDOR:
                return self._extrair_com(self._modelo_relevo, self._reduzir(bgr), **PARAMETROS_SERVIDOR)
            linhas = self._extrair(bgr)
            if self._usa_relevo(campo):
                import cv2
                g = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
                cinza = cv2.cvtColor(g, cv2.COLOR_GRAY2BGR)
                # Acrescenta; não substitui resultados nem monta valores a partir de caracteres
                # isolados. O Paddle devolve as regiões na escala de entrada, preservada na conversão.
                linhas.extend(self._extrair(cinza, **PARAMETROS_RELEVO))
                if self._usa_morfologico(campo):
                    linhas.extend(self._extrair(self._realce_relevo(g), **PARAMETROS_MORFOLOGICO))
            return linhas
        finally:
            self._lock.release()

    # Reduz ANTES de entregar ao Paddle: o parâmetro text_det_limit_side_len por si só não
    # evita o pico de memória de decodificar e it copiar uma imagem de câmera em resolução
    # total (até 4080x3072 nas fotos reais) antes do redimensionamento interno do detector.
    def _reduzir(self, bgr):
        import cv2
        altura, largura = bgr.shape[:2]
        maior_lado = max(altura, largura)
        if maior_lado <= LADO_MAXIMO_SERVIDOR:
            return bgr
        escala = LADO_MAXIMO_SERVIDOR / maior_lado
        nova_dimensao = (round(largura * escala), round(altura * escala))
        return cv2.resize(bgr, nova_dimensao, interpolation=cv2.INTER_AREA)

    def _usa_relevo(self, campo):
        if campo == 'DOT':
            return self.perfil_dot != 'legado'
        return campo in CAMPOS_RELEVO and self.perfil_relevo != 'legado'

    # Só para os campos de alfabeto grande vazado (não o DOT, que já tem sua própria passagem
    # ajustada e comprovada em produção): letra em relevo iluminada de lado forma uma borda clara
    # de um lado do traço e uma escura do outro. Top-hat realça a clara, black-hat a escura; somar
    # os dois mantém o traço legível qualquer que seja o lado de onde veio a luz.
    def _usa_morfologico(self, campo):
        return campo in CAMPOS_RELEVO and self.perfil_morfologico != 'legado'

    def _realce_relevo(self, cinza):
        import cv2
        lado = max(cinza.shape)
        tamanho_nucleo = max(9, min(31, lado // 60))
        if tamanho_nucleo % 2 == 0:
            tamanho_nucleo += 1
        nucleo = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (tamanho_nucleo, tamanho_nucleo))
        tophat = cv2.morphologyEx(cinza, cv2.MORPH_TOPHAT, nucleo)
        blackhat = cv2.morphologyEx(cinza, cv2.MORPH_BLACKHAT, nucleo)
        realce = cv2.normalize(cv2.add(tophat, blackhat), None, 0, 255, cv2.NORM_MINMAX)
        return cv2.cvtColor(realce.astype('uint8'), cv2.COLOR_GRAY2BGR)

    def _extrair(self, imagem, **parametros):
        return self._extrair_com(self._modelo, imagem, **parametros)

    def _extrair_com(self, modelo, imagem, **parametros):
        linhas = []
        for resultado in modelo.predict(imagem, **parametros):
            for texto, score, poligono in zip(
                resultado['rec_texts'], resultado['rec_scores'], resultado['rec_polys']
            ):
                linhas.append({'texto': texto, 'escore': float(score), 'regiao': poligono.tolist()})
        return linhas
