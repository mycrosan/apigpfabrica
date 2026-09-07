"""Recorta as regiões propostas para tornar a revisão cega executável.

A planilha cega pede que o revisor localize um polígono por coordenadas dentro de uma foto
de milhares de pixels. Sem recorte, conferir 509 regiões à mão é inviável e o revisor acaba
transcrevendo pela memória do cadastro em vez do que a imagem mostra — que é justamente o
viés que a revisão cega existe para evitar.

Este script produz uma imagem por região e acrescenta as colunas `recorte` e `triagem`.
A transcrição proposta pelo OCR nunca é lida nem escrita aqui: a revisão continua cega.
Originais são apenas lidos.

As coordenadas do detector estão no espaço da imagem **já rotacionada** pelo EXIF, porque o
serviço aplica `exif-transpose` antes de inferir. Recortar o arquivo cru desalinha a região
e, em fotos deitadas, chega a produzir caixa invertida. Por isso a rotação é aplicada aqui
antes de qualquer corte.
"""
import argparse
import csv
import json
from pathlib import Path

from PIL import Image, ImageOps

COLUNAS_NOVAS = ('recorte', 'triagem', 'lado_menor_px')
SEM_REGIAO = 'SEM_REGIAO'
RUIDO_PROVAVEL = 'RUIDO_PROVAVEL'
REVISAR = 'REVISAR'
FALHA_RECORTE = 'FALHA_RECORTE'


def caixa(pontos):
    """Converte o polígono do detector no retângulo que o contém."""
    xs = [ponto[0] for ponto in pontos]
    ys = [ponto[1] for ponto in pontos]
    return min(xs), min(ys), max(xs), max(ys)


def expandir(limites, margem, largura, altura):
    """Acrescenta contexto ao redor da região, sem sair da imagem.

    Uma caixa justa corta as bordas das letras e faz o revisor julgar pior do que a foto
    permite. A margem é proporcional ao lado da caixa, com um mínimo em pixels para que
    regiões pequenas também ganhem contexto.
    """
    esquerda, topo, direita, base = limites
    folga_x = max(int((direita - esquerda) * margem), 8)
    folga_y = max(int((base - topo) * margem), 8)
    return (max(esquerda - folga_x, 0), max(topo - folga_y, 0),
            min(direita + folga_x, largura), min(base + folga_y, altura))


def ampliar(recorte, lado_visivel):
    """Amplia recortes pequenos para que possam ser olhados.

    Ampliar não acrescenta informação: serve só para o revisor conseguir ver o que existe
    e decidir com honestidade se está legível.
    """
    menor = min(recorte.size)
    if menor >= lado_visivel or menor == 0:
        return recorte
    fator = lado_visivel / menor
    novo = (max(int(recorte.width * fator), 1), max(int(recorte.height * fator), 1))
    return recorte.resize(novo, Image.LANCZOS)


def recortar(planilha, fotos, destino, margem=0.25, lado_minimo=30, lado_visivel=160):
    """Gera um recorte por região e devolve a planilha enriquecida.

    :param planilha: CSV cego de entrada, preservado sem alteração.
    :param fotos: raiz das fotos originais, apenas lida.
    :param destino: pasta onde os recortes e a nova planilha são gravados.
    :return: contagens por triagem.
    """
    destino = Path(destino)
    imagens = destino / 'recortes'
    imagens.mkdir(parents=True, exist_ok=True)
    with Path(planilha).open(encoding='utf-8') as arquivo:
        linhas = list(csv.DictReader(arquivo))
        colunas = list(linhas[0].keys()) if linhas else []
    contagem = {SEM_REGIAO: 0, RUIDO_PROVAVEL: 0, REVISAR: 0, FALHA_RECORTE: 0}
    abertas = {}
    for linha in linhas:
        pontos = json.loads(linha.get('regiao_json') or 'null')
        if not pontos:
            linha['recorte'] = ''
            linha['triagem'] = SEM_REGIAO
            linha['lado_menor_px'] = ''
            contagem[SEM_REGIAO] += 1
            continue
        origem = Path(fotos) / linha['arquivo']
        try:
            if origem not in abertas:
                with Image.open(origem) as aberta:
                    # A mesma rotação que o serviço aplica antes da inferência.
                    abertas[origem] = ImageOps.exif_transpose(aberta).convert('RGB')
            imagem = abertas[origem]
            bruta = caixa(pontos)
            limites = expandir(bruta, margem, imagem.width, imagem.height)
            if limites[0] >= limites[2] or limites[1] >= limites[3]:
                raise ValueError('regiao fora da imagem')
            recorte = imagem.crop(limites)
            # A triagem usa a caixa proposta, não o recorte com margem: é o texto que precisa ser legível.
            menor = min(bruta[2] - bruta[0], bruta[3] - bruta[1])
            nome = '{}_{}.jpg'.format(linha['imagem_sha256'][:12], linha['regiao_indice'])
            ampliar(recorte, lado_visivel).save(imagens / nome, 'JPEG', quality=92)
        except (OSError, ValueError) as erro:
            # Foto ausente ou ilegível não invalida a planilha: a linha fica marcada para investigação.
            linha['recorte'] = ''
            linha['triagem'] = FALHA_RECORTE
            linha['lado_menor_px'] = ''
            linha['observacao_recorte'] = '{}: {}'.format(type(erro).__name__, erro)
            contagem[FALHA_RECORTE] += 1
            continue
        triagem = RUIDO_PROVAVEL if menor < lado_minimo else REVISAR
        linha['recorte'] = 'recortes/' + nome
        linha['triagem'] = triagem
        linha['lado_menor_px'] = str(menor)
        contagem[triagem] += 1
    saida = destino / 'revisao_cega_com_recortes.csv'
    campos = colunas + [coluna for coluna in COLUNAS_NOVAS if coluna not in colunas]
    if any('observacao_recorte' in linha for linha in linhas):
        campos.append('observacao_recorte')
    with saida.open('w', encoding='utf-8', newline='') as arquivo:
        escritor = csv.DictWriter(arquivo, fieldnames=campos, extrasaction='ignore')
        escritor.writeheader()
        escritor.writerows(linhas)
    return contagem


def main():
    analisador = argparse.ArgumentParser(description=__doc__)
    analisador.add_argument('--planilha', required=True)
    analisador.add_argument('--fotos', required=True)
    analisador.add_argument('--destino', required=True)
    analisador.add_argument('--margem', type=float, default=0.25)
    analisador.add_argument('--lado-minimo', type=int, default=30,
                            help='Abaixo disto a região é marcada como ruído provável, não removida.')
    argumentos = analisador.parse_args()
    contagem = recortar(argumentos.planilha, argumentos.fotos, argumentos.destino,
                        argumentos.margem, argumentos.lado_minimo)
    for chave in (REVISAR, RUIDO_PROVAVEL, SEM_REGIAO, FALHA_RECORTE):
        print('{:<16} {}'.format(chave, contagem[chave]))


if __name__ == '__main__':
    main()
