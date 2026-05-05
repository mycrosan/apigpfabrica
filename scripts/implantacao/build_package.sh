#!/usr/bin/env bash
set -euo pipefail

# Empacota a aplicação para implantação em produção.
# Gera um pacote tar.gz com estrutura:
#   app/      -> JAR da aplicação
#   config/   -> application.properties (template)
#   systemd/  -> arquivo de serviço systemd
#   README_DEPLOY.txt -> instruções de implantação

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$PROJECT_ROOT"

POM_FILE="pom.xml"             # POM principal (gera JAR por padrão)
SPRING_PROFILE="prod"          # Perfil de produção
OUTPUT_DIR="release"           # Diretório de saída do pacote

mkdir -p "$OUTPUT_DIR"

echo "[INFO] Coletando metadados do projeto (artifactId e version)"
ARTIFACT_ID=$(mvn -q -DforceStdout -f "$POM_FILE" help:evaluate -Dexpression=project.artifactId)
VERSION=$(mvn -q -DforceStdout -f "$POM_FILE" help:evaluate -Dexpression=project.version)

echo "[INFO] Construindo artefato de produção (pom: $POM_FILE, profile: $SPRING_PROFILE)"
mvn -f "$POM_FILE" clean package -DskipTests -Dspring.profiles.active="$SPRING_PROFILE"

JAR_PATH="target/${ARTIFACT_ID}-${VERSION}.jar"
WAR_PATH="target/${ARTIFACT_ID}-${VERSION}.war"
if [[ ! -f "$JAR_PATH" && ! -f "$WAR_PATH" ]]; then
  echo "[ERRO] Artefato não encontrado em: $JAR_PATH nem $WAR_PATH"
  echo "       Verifique se o build gerou o artefato corretamente."
  exit 1
fi

DIST_DIR="${OUTPUT_DIR}/${ARTIFACT_ID}-${VERSION}"
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR/app" "$DIST_DIR/config" "$DIST_DIR/systemd"

echo "[INFO] Copiando artefato para $DIST_DIR/app/"
if [[ -f "$JAR_PATH" ]]; then
  cp "$JAR_PATH" "$DIST_DIR/app/"
else
  cp "$WAR_PATH" "$DIST_DIR/app/"
fi

echo "[INFO] Gerando template de configuração em $DIST_DIR/config/application.properties"
cat > "$DIST_DIR/config/application.properties" <<'EOF'
# ==== Configuração de Produção (exemplo) ====

# Banco de Dados MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/gp_premium?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo
spring.datasource.username=SEU_USUARIO
spring.datasource.password=SUA_SENHA
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Hibernate / JPA
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# Perfil
spring.profiles.active=prod

# JWT / Segurança
api.jwt.secret=ALTERE_PARA_UM_SEGREDO_FORTE
api.jwt.expiration=86400000

# Servidor
server.port=8080

# Logs
logging.level.root=INFO
EOF

SERVICE_SRC="scripts/gp-spring-boot.service"
SERVICE_DST="$DIST_DIR/systemd/gp-spring-boot.service"
if [[ -f "$SERVICE_SRC" ]]; then
  echo "[INFO] Copiando service systemd existente para $SERVICE_DST"
  cp "$SERVICE_SRC" "$SERVICE_DST"
else
  echo "[INFO] Criando service systemd padrão em $SERVICE_DST"
  cat > "$SERVICE_DST" <<EOF
[Unit]
Description=GP Premium API (Spring Boot)
After=network.target

[Service]
Type=simple
User=gp
Group=gp
EnvironmentFile=/etc/gp/application.properties
ExecStart=/usr/bin/java -jar /opt/gp/app/${ARTIFACT_ID}-${VERSION}.jar
Restart=always
RestartSec=10
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
EOF
fi

echo "[INFO] Escrevendo instruções de implantação em $DIST_DIR/README_DEPLOY.txt"
cat > "$DIST_DIR/README_DEPLOY.txt" <<EOF
GP Premium API - Pacote de Implantação
======================================

Conteúdo:
- app/${ARTIFACT_ID}-${VERSION}.jar
- config/application.properties (ajuste credenciais e segredos)
- systemd/gp-spring-boot.service

Pré-requisitos:
- Java 11+ instalado (/usr/bin/java)
- MySQL 8.x com banco criado (ex.: gp_premium) e usuário com permissões

Passos de implantação (Linux):
1) Criar diretórios padrão:
   sudo mkdir -p /opt/gp/app /etc/gp
   sudo chown -R gp:gp /opt/gp /etc/gp

2) Copiar arquivos:
   sudo cp app/${ARTIFACT_ID}-${VERSION}.jar /opt/gp/app/
   sudo cp config/application.properties /etc/gp/

3) Instalar serviço systemd:
   sudo cp systemd/gp-spring-boot.service /etc/systemd/system/
   sudo systemctl daemon-reload
   sudo systemctl enable gp-spring-boot
   sudo systemctl start gp-spring-boot

4) Logs e verificação:
   sudo journalctl -u gp-spring-boot -f
   A API deve subir em http://SEU_HOST:8080/

Observações:
- Flyway executará migrações automaticamente ao iniciar se spring.flyway.enabled=true.
- Ajuste server.port e variáveis de JWT conforme necessário.
- Para atualizar, substitua o JAR em /opt/gp/app e reinicie o serviço.
EOF

echo "[INFO] Gerando arquivo .tar.gz do pacote"
(cd "$OUTPUT_DIR" && tar -czf "${ARTIFACT_ID}-${VERSION}.tar.gz" "${ARTIFACT_ID}-${VERSION}")

echo "[SUCESSO] Pacote gerado: $OUTPUT_DIR/${ARTIFACT_ID}-${VERSION}.tar.gz"