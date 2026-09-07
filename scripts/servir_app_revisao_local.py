#!/usr/bin/env python3
"""Serve o Flutter Web e encaminha /api apenas para a API do piloto em localhost."""
import argparse
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import urllib.error
import urllib.request


class ServidorLocal(SimpleHTTPRequestHandler):
    def log_message(self, formato, *argumentos):
        # Não registrar credenciais, corpos nem parâmetros de requisições.
        pass

    def do_GET(self):
        if self.path.startswith("/api/"):
            self.encaminhar()
            return
        super().do_GET()

    def do_POST(self):
        self.encaminhar()

    def do_PUT(self):
        self.encaminhar()

    def encaminhar(self):
        if not self.path.startswith("/api/"):
            self.send_error(404)
            return
        tamanho = int(self.headers.get("Content-Length", "0"))
        if tamanho < 0 or tamanho > 16 * 1024 * 1024:
            self.send_error(413)
            return
        corpo = self.rfile.read(tamanho) if tamanho else None
        cabecalhos = {chave: self.headers[chave] for chave in
                     ("Authorization", "Content-Type", "Idempotency-Key", "Accept") if chave in self.headers}
        pedido = urllib.request.Request("http://127.0.0.1:8092" + self.path,
            data=corpo, headers=cabecalhos, method=self.command)
        try:
            resposta = urllib.request.urlopen(pedido, timeout=120)
        except urllib.error.HTTPError as erro:
            resposta = erro
        except OSError:
            self.send_error(503, "API local indisponivel")
            return
        with resposta:
            dados = resposta.read()
            self.send_response(resposta.code)
            self.send_header("Content-Type", resposta.headers.get("Content-Type", "application/json"))
            self.send_header("Content-Length", str(len(dados)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(dados)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--web", required=True, type=Path)
    argumentos = parser.parse_args()
    raiz = argumentos.web.resolve(strict=True)
    if not (raiz / "main.dart.js").is_file():
        raise SystemExit("Compile o Flutter Web antes de iniciar o piloto.")
    servidor = ThreadingHTTPServer(("127.0.0.1", 8093),
        lambda *args, **kwargs: ServidorLocal(*args, directory=str(raiz), **kwargs))
    print("Aplicativo local: http://127.0.0.1:8093", flush=True)
    servidor.serve_forever()
