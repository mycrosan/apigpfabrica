#!/usr/bin/env python3
"""Prepara um piloto somente em localhost, com banco próprio e credenciais geradas."""
import argparse
import base64
import json
import os
from pathlib import Path
import secrets
import subprocess
import time
import urllib.request


def gravar_privado(arquivo, texto):
    with arquivo.open("w") as saida:
        os.chmod(arquivo, 0o600)
        saida.write(texto)


def executar(argumentos, **opcoes):
    return subprocess.run(argumentos, check=True, capture_output=True, text=True, **opcoes).stdout


def preparar(diretorio):
    diretorio.mkdir(parents=True, exist_ok=True)
    configuracao = diretorio / "credenciais.json"
    if not configuracao.exists():
        dados = {"banco": secrets.token_hex(24), "root": secrets.token_hex(24),
                 "jwt": base64.b64encode(secrets.token_bytes(48)).decode(),
                 "usuarios": {nome: secrets.token_hex(8) for nome in ("revisor1", "revisor2", "revisor3")}}
        gravar_privado(configuracao, json.dumps(dados, indent=2))
    dados = json.loads(configuracao.read_text())
    ambiente_banco = diretorio / "mysql.env"
    gravar_privado(ambiente_banco, "MYSQL_DATABASE=revisao_local\nMYSQL_USER=revisao\n"
                  f"MYSQL_PASSWORD={dados['banco']}\nMYSQL_ROOT_PASSWORD={dados['root']}\n")
    nome = "fabrica-revisao-local"
    encontrado = subprocess.run(["docker", "inspect", nome], capture_output=True, text=True)
    if encontrado.returncode == 0:
        atual = json.loads(encontrado.stdout)[0]
        if atual["Config"].get("Labels", {}).get("fabrica.piloto") != "revisao-local":
            raise RuntimeError("Já existe um container com esse nome sem identificação deste piloto.")
        executar(["docker", "start", nome])
    else:
        executar(["docker", "run", "-d", "--name", nome, "--label", "fabrica.piloto=revisao-local",
                  "-p", "127.0.0.1:13316:3306", "--env-file", str(ambiente_banco),
                  "-v", str(diretorio / "mysql") + ":/var/lib/mysql", "mysql:8.4.6"])
    for tentativa in range(60):
        teste = subprocess.run(["docker", "exec", nome, "mysqladmin", "ping", "--silent"], capture_output=True)
        if teste.returncode == 0:
            break
        time.sleep(1)
    raiz = Path(__file__).resolve().parents[1]
    java_raiz = os.environ.get("JAVA_HOME")
    if not java_raiz:
        raise RuntimeError("Ative Java 17 usando SDKMAN e sdk env antes de executar.")
    ambiente = dict(os.environ, DATABASE_URL="jdbc:mysql://127.0.0.1:13316/revisao_local?serverTimezone=UTC",
        DATABASE_USERNAME="revisao", DATABASE_PASSWORD=dados["banco"], JWT_SECRET=dados["jwt"],
        SERVER_PORT="8092", SERVER_ADDRESS="127.0.0.1", REVISAO_COLETA="false",
        REVISAO_DIRETORIO=str(diretorio / "evidencias"), EVIDENCIAS_DIRETORIO=str(diretorio / "capturas"))
    ambiente.pop("SPRING_PROFILES_ACTIVE", None)
    registro = (diretorio / "api.log").open("a")
    processo = subprocess.Popen([str(Path(java_raiz) / "bin/java"), "-Xmx1536m", "-jar",
        str(raiz / "target/gp-premium-1.11.0.war")], env=ambiente, stdout=registro,
        stderr=subprocess.STDOUT, cwd=raiz, start_new_session=True)
    (diretorio / "api.pid").write_text(str(processo.pid))
    for tentativa in range(60):
        if processo.poll() is not None:
            raise RuntimeError("A API não iniciou. Consulte api.log; não foram alterados bancos externos.")
        try:
            urllib.request.urlopen("http://127.0.0.1:8092/api/status", timeout=1)
            break
        except OSError:
            time.sleep(1)
    for login, senha in dados["usuarios"].items():
        senha_hash = executar(["htpasswd", "-niBC", "10", login], input=senha + "\n").strip().split(":", 1)[1]
        # Valores gerados pelo script e nomes fixos; nunca recebe SQL do manifesto ou de fotos.
        consulta = (f"INSERT INTO usuario (login,nome,senha) SELECT '{login}','Revisor local','{senha_hash}' "
            f"WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE login='{login}');\n"
            f"INSERT INTO usuario_perfil (usuario_id,perfil_id) SELECT u.id,p.id FROM usuario u,perfil p "
            f"WHERE u.login='{login}' AND p.descricao='REVISAR_LEITURA' AND NOT EXISTS "
            "(SELECT 1 FROM usuario_perfil up WHERE up.usuario_id=u.id AND up.perfil_id=p.id);\n")
        if login == "revisor1":
            consulta += ("INSERT INTO usuario_perfil (usuario_id,perfil_id) SELECT u.id,p.id FROM usuario u,perfil p "
                "WHERE u.login='revisor1' AND p.descricao='IMPORTAR_LEITURA' AND NOT EXISTS "
                "(SELECT 1 FROM usuario_perfil up WHERE up.usuario_id=u.id AND up.perfil_id=p.id);\n")
        executar(["docker", "exec", "-i", nome, "sh", "-c",
                  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot revisao_local'], input=consulta)
        requisicao = urllib.request.Request("http://127.0.0.1:8092/api/auth",
            data=json.dumps({"login": login, "senha": senha}).encode(),
            headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(requisicao, timeout=10) as resposta:
            token = json.load(resposta)["token"]
        gravar_privado(diretorio / (login + ".token"), token)
    instrucoes = "# Acesso ao piloto local\n\nAPI: http://127.0.0.1:8092/api/\n\n"
    for login, senha in dados["usuarios"].items():
        instrucoes += f"- Usuário `{login}` — senha `{senha}`\n"
    instrucoes += "\nUse uma conta por pessoa. Não preencha a segunda revisão em nome de outra pessoa.\n"
    gravar_privado(diretorio / "ACESSO_LOCAL.md", instrucoes)
    print("Piloto disponível somente em localhost. Acesso salvo em " + str(diretorio / "ACESSO_LOCAL.md"))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--diretorio", required=True, type=Path)
    preparar(parser.parse_args().diretorio.resolve())
