"""Carregamento de pesos locais e extração sem interpretação de catálogo."""
import hashlib
import json
import os
from pathlib import Path
from threading import Lock

PERFIL_DOT = 'dot-relevo-v1'
PARAMETROS_DOT = {
    'text_det_limit_side_len': 960,
    'text_det_limit_type': 'max',
    'text_det_thresh': 0.15,
    'text_det_box_thresh': 0.3,
    'text_det_unclip_ratio': 2.0,
}


class MotorLocal:
    def __init__(self, diretorio: Path):
        self.perfil_dot = os.getenv('OCR_PERFIL_DOT', PERFIL_DOT)
        if self.perfil_dot not in ('legado', PERFIL_DOT):
            raise ValueError('Perfil DOT desconhecido')
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
        # Desativa verificações de origem: todos os modelos são fornecidos por caminho local.
        os.environ['PADDLE_PDX_DISABLE_MODEL_SOURCE_CHECK'] = 'True'
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
        if self.perfil_dot != 'legado':
            perfil = json.dumps({'perfil': self.perfil_dot, 'parametros': PARAMETROS_DOT,
                                 'conversao': 'opencv-bgr-gray-bgr', 'estrategia': 'original-mais-cinza'},
                                sort_keys=True).encode()
            self.versao = hashlib.sha256(conteudo + perfil).hexdigest()

    def preprocessamento(self, campo):
        if campo == 'DOT' and self.perfil_dot != 'legado':
            return 'exif-transpose-rgb-' + self.perfil_dot
        return 'exif-transpose-rgb-v1'

    def reconhecer(self, imagem, campo=None):
        import numpy as np
        if not self._lock.acquire(blocking=False):
            raise BlockingIOError('OCR ocupado')
        try:
            # Paddle espera BGR quando recebe ndarray.
            bgr = np.asarray(imagem)[:, :, ::-1].copy()
            linhas = self._extrair(bgr)
            if campo == 'DOT' and self.perfil_dot != 'legado':
                import cv2
                cinza = cv2.cvtColor(cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY), cv2.COLOR_GRAY2BGR)
                # Não substitui resultados nem monta números a partir de caracteres isolados.
                # O Paddle devolve as regiões na escala de entrada, preservada na conversão.
                linhas.extend(self._extrair(cinza, **PARAMETROS_DOT))
            return linhas
        finally:
            self._lock.release()

    def _extrair(self, imagem, **parametros):
        linhas = []
        for resultado in self._modelo.predict(imagem, **parametros):
            for texto, score, poligono in zip(
                resultado['rec_texts'], resultado['rec_scores'], resultado['rec_polys']
            ):
                linhas.append({'texto': texto, 'escore': float(score), 'regiao': poligono.tolist()})
        return linhas
