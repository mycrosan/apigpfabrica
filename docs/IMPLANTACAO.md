# Manual de Implantação (Produção)

Este guia descreve passo a passo como empacotar, transferir e executar a API GP Premium em um servidor Linux com MySQL.

## 1. Pré-requisitos

- Sistema: Linux com systemd (Ubuntu, Debian, CentOS, etc.)
- Java 11+ instalado (verifique com `java -version`)
- MySQL 8.x instalado e acessível
- Usuário com permissão de sudo para instalar serviços
- Porta 8080 liberada (ou ajuste `server.port`)

## 2. Build e empacotamento

Opção A: Script de empacotamento (recomendado)
1. No diretório do projeto, execute:
   ```bash
   bash scripts/implantacao/build_package.sh
   ```
2. Ao finalizar, o pacote estará em `release/<artifactId>-<version>.tar.gz` (ex.: `release/gp-premium-0.0.35-SNAPSHOT.tar.gz`).

Opção B: Manual
1. Build do artefato:
   ```bash
   mvn clean package -DskipTests -Dspring.profiles.active=prod
   ```
2. Crie uma estrutura de distribuição contendo:
   - app/ -> copie o JAR gerado em `target/<artifactId>-<version>.jar`
   - config/ -> crie `application.properties` de produção
   - systemd/ -> adicione `gp-spring-boot.service`
   - README_DEPLOY.txt -> instruções
3. Compacte em tar.gz:
   ```bash
   tar -czf <artifactId>-<version>.tar.gz <artifactId>-<version>/
   ```

## 3. Banco de Dados MySQL

1. Acesse o MySQL como root/admin:
   ```sql
   CREATE DATABASE gp_premium CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'gpuser'@'%' IDENTIFIED BY 'SUA_SENHA_FORTE';
   GRANT ALL PRIVILEGES ON gp_premium.* TO 'gpuser'@'%';
   FLUSH PRIVILEGES;
   ```
2. Opcional: restrinja o acesso do usuário ao host específico (substitua `%` pelo IP/host).

## 4. Transferência para o servidor

1. Copie o tar.gz para o servidor (ex.: via scp):
   ```bash
   scp release/<artifactId>-<version>.tar.gz user@server:/tmp/
   ```
2. No servidor, descompacte:
   ```bash
   cd /tmp
   tar -xzf <artifactId>-<version>.tar.gz
   ```

## 5. Estrutura de diretórios e permissões

1. Crie diretórios padrão:
   ```bash
   sudo mkdir -p /opt/gp/app /etc/gp
   ```
2. Ajuste proprietário (ex.: usuário de execução `gp`):
   ```bash
   sudo useradd -r -s /bin/false gp || true
   sudo chown -R gp:gp /opt/gp /etc/gp
   ```

## 6. Configuração de aplicação

1. Copie o JAR e o `application.properties`:
   ```bash
   sudo cp /tmp/<artifactId>-<version>/app/<artifactId>-<version>.jar /opt/gp/app/
   sudo cp /tmp/<artifactId>-<version>/config/application.properties /etc/gp/
   ```
2. Edite `/etc/gp/application.properties` e ajuste:
   ```properties
   # Banco de Dados MySQL
   spring.datasource.url=jdbc:mysql://HOST:3306/gp_premium?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo
   spring.datasource.username=gpuser
   spring.datasource.password=SUA_SENHA_FORTE
   spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

   # JPA / Hibernate
   spring.jpa.hibernate.ddl-auto=none
   spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
   spring.jpa.show-sql=false

   # Flyway
   spring.flyway.enabled=true
   spring.flyway.locations=classpath:db/migration

   # Perfil e servidor
   spring.profiles.active=prod
   server.port=8080

   # Segurança (JWT)
   api.jwt.secret=ALTERE_PARA_UM_SEGREDO_FORTE
   api.jwt.expiration=86400000

   # Logs
   logging.level.root=INFO
   ```

## 7. Serviço systemd

1. Copie o service:
   ```bash
   sudo cp /tmp/<artifactId>-<version>/systemd/gp-spring-boot.service /etc/systemd/system/
   ```
2. Conteúdo de referência (`gp-spring-boot.service`):
   ```ini
   [Unit]
   Description=GP Premium API (Spring Boot)
   After=network.target

   [Service]
   Type=simple
   User=gp
   Group=gp
   EnvironmentFile=/etc/gp/application.properties
   ExecStart=/usr/bin/java -jar /opt/gp/app/<artifactId>-<version>.jar
   Restart=always
   RestartSec=10
   SuccessExitStatus=143

   [Install]
   WantedBy=multi-user.target
   ```
3. Ative e inicialize:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable gp-spring-boot
   sudo systemctl start gp-spring-boot
   ```

## 8. Verificação e logs

1. Verifique status:
   ```bash
   systemctl status gp-spring-boot
   ```
2. Acompanhe logs:
   ```bash
   sudo journalctl -u gp-spring-boot -f
   ```
3. A API deve responder em `http://SEU_HOST:8080/`
4. Swagger UI (se habilitado): `http://SEU_HOST:8080/swagger-ui/index.html`

## 9. Migrações Flyway

- Com `spring.flyway.enabled=true`, as migrações em `classpath:db/migration` serão aplicadas automaticamente no start.
- Garanta que o usuário do MySQL tenha permissões para criar/alterar tabelas e chaves.

## 10. Atualização de versão

1. Pare o serviço:
   ```bash
   sudo systemctl stop gp-spring-boot
   ```
2. Substitua o JAR em `/opt/gp/app/` pela nova versão.
3. Inicie novamente:
   ```bash
   sudo systemctl start gp-spring-boot
   ```

## 11. Rollback

1. Mantenha cópia do JAR anterior.
2. Em caso de problemas, pare o serviço e restaure o JAR antigo.
3. Se necessário, use backups do banco para reverter dados.

## 12. Segurança

- Use um `api.jwt.secret` forte e não versionado.
- Restrinja acesso ao banco por IP/host.
- Considere um proxy reverso (Nginx) com TLS.

## 13. Dicas e troubleshooting

- Erros de conexão MySQL: revise `spring.datasource.url`, `username`, `password`, `allowPublicKeyRetrieval`, `serverTimezone`.
- Falhas em migrações: confira logs do Flyway e o conteúdo de `src/main/resources/db/migration`.
- Porta em uso: ajuste `server.port` ou libere a porta.
- Testes locais usam H2 e desabilitam Flyway (perfil `test`), produção usa MySQL e Flyway.

## 14. Deploy em WildFly (WAR) – Opcional

- Caso deseje WAR, utilize `pom_prd.xml` (packaging `war`) e plugin do WildFly.
- Ajuste dependências de Swagger/OpenAPI no `pom_prd.xml` se necessário.
- Realize deploy via `wildfly-maven-plugin` ou console do WildFly.

## 15. Checklist de implantação

- [ ] Java 11+ instalado
- [ ] Banco `gp_premium` criado e usuário com permissões
- [ ] `application.properties` configurado em `/etc/gp/`
- [ ] JAR copiado para `/opt/gp/app/`
- [ ] Service em `/etc/systemd/system/gp-spring-boot.service`
- [ ] Serviço iniciado e respondendo
- [ ] Migrações Flyway aplicadas sem erros
- [ ] Logs sem erros críticos