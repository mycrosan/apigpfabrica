"""Carregamento de pesos locais e extração sem interpretação de catálogo."""
import hashlib
import json
import os
from pathlib import Path
from threading import Lock


class MotorLocal:
    def __init__(self, diretorio: Path):
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

    def reconhecer(self, imagem):
        import numpy as np
        if not self._lock.acquire(blocking=False):
            raise BlockingIOError('OCR ocupado')
        try:
            # Paddle espera BGR quando recebe ndarray.
            resultados = self._modelo.predict(np.asarray(imagem)[:, :, ::-1])
            linhas = []
            for resultado in resultados:
                for texto, score, poligono in zip(
                    resultado['rec_texts'], resultado['rec_scores'], resultado['rec_polys']
                ):
                    linhas.append({'texto': texto, 'escore': float(score), 'regiao': poligono.tolist()})
            return linhas
        finally:
            self._lock.release()
