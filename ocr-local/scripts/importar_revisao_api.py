#!/usr/bin/env python3
"""Envia evidências não revisadas à API própria, sem atribuir rótulos humanos."""
import argparse
import base64
import hashlib
import ipaddress
import json
import os
from pathlib import Path
import socket
import urllib.error
import urllib.parse
import urllib.request


def verificar_destino(endereco):
    partes = urllib.parse.urlsplit(endereco)
    if partes.scheme not in ("http", "https") or not partes.hostname or partes.username:
        raise ValueError("Use uma URL HTTP(S) da API na infraestrutura própria.")
    for resultado in socket.getaddrinfo(partes.hostname, partes.port or 80):
        if not ipaddress.ip_address(resultado[4][0]).is_private:
            raise ValueError("O importador aceita somente a API na rede privada ou localhost.")


def importar(manifesto, fotos, destino, token, progresso):
    verificar_destino(destino)
    raiz = fotos.resolve(strict=True)
    manifesto_hash = hashlib.sha256(manifesto.read_bytes()).hexdigest()
    token = token.read_text().strip()
    if not token:
        raise ValueError("Arquivo de token vazio.")
    registros = []
    for numero, linha in enumerate(manifesto.read_text().splitlines(), start=1):
        entrada = json.loads(linha)
        if entrada.get("http_status") != 200:
            continue
        foto = (raiz / entrada["arquivo"]).resolve(strict=True)
        if not foto.is_relative_to(raiz):
            raise ValueError("Referência de foto fora da pasta informada.")
        conteudo = foto.read_bytes()
        imagem_hash = hashlib.sha256(conteudo).hexdigest()
        if imagem_hash != entrada["imagem_sha256"]:
            raise ValueError(f"Foto alterada desde a extração, linha {numero}.")
        dados = {"imagemSha256": imagem_hash, "manifestoSha256": manifesto_hash,
                 "fotoBase64": base64.b64encode(conteudo).decode(),
                 "extracao": entrada["resposta_ocr_nao_revisada"]}
        # O campo usado para chamar o extrator não se torna campo rotulado no banco.
        requisicao = urllib.request.Request(destino.rstrip("/") + "/v2/revisoes-leitura/importacoes",
            data=json.dumps(dados).encode(), headers={"Authorization": "Bearer " + token,
                "Content-Type": "application/json"}, method="POST")
        try:
            with urllib.request.urlopen(requisicao, timeout=120) as resposta:
                resultado = json.load(resposta)
        except urllib.error.HTTPError as erro:
            raise RuntimeError(f"A API rejeitou a linha {numero}: HTTP {erro.code}. Confira permissão, limites e integridade.") from erro
        registros.append({"imagem_sha256": imagem_hash, "itens": resultado["itens"]})
        temporario = progresso.with_suffix(".tmp")
        temporario.write_text(json.dumps({"manifesto_sha256": manifesto_hash, "importacoes": registros}, indent=2))
        os.chmod(temporario, 0o600)
        temporario.replace(progresso)
        print(f"Foto {numero}: {len(resultado['itens'])} regiões pendentes.", flush=True)
    return registros


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifesto", type=Path, required=True)
    parser.add_argument("--fotos", type=Path, required=True)
    parser.add_argument("--api", required=True, help="URL terminando em /api")
    parser.add_argument("--token-arquivo", type=Path, required=True)
    parser.add_argument("--progresso", type=Path, required=True)
    argumentos = parser.parse_args()
    registros = importar(argumentos.manifesto, argumentos.fotos, argumentos.api,
                         argumentos.token_arquivo, argumentos.progresso)
    print(f"Concluído: {len(registros)} fotos. Nenhuma transcrição aprovada automaticamente.")


if __name__ == "__main__":
    main()
