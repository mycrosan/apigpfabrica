# Java 17, Jakarta e banco versionado

A API usa SDKMAN (`.sdkmanrc`, Java 17.0.16-tem), Spring Boot 3.5.16, Spring Security 6 com SecurityFilterChain, MapStruct 1.6.3 e Maven Wrapper 3.9.11. Runtime Docker usa Java 17. Rotas de carcaça preservam envelope e status de sucesso do cliente legado; v2 possui contrato próprio.

## Configuração

`application.yml` exige DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD e JWT_SECRET do ambiente. Não carrega credenciais antigas dos arquivos properties. Endpoints legados ainda dependem de open-in-view; novos serviços convertem entidades em DTO dentro da transação. A migração completa desses endpoints não é parte do cadastro de pneus.

## Flyway

V1_7_1 cria a estrutura completa para instalação nova. V1_7_2 já existente foi preservada. V1_7_3 garante IDs de status usados pelo fluxo, sem substituir descrições existentes. Hibernate apenas valida o esquema em produção; nenhuma atualização automática é permitida.

Banco existente: inventariar estrutura e histórico Flyway, conferir diferenças contra a baseline, realizar backup e ensaiar atualização numa cópia descartável. Não habilitar baseline-on-migrate. Registrar baseline explícito 1.7.1 somente se a estrutura corresponder; se já houver histórico, comparar versões/checksums antes de prosseguir. Não executar repair ou ignorar migrações falhas automaticamente.

Instalação nova: configurar banco vazio; Flyway executa scripts em ordem. Testes Testcontainers usam MySQL 8.4.6 vazio e verificam migrations mais Hibernate validate. Testes legados H2 isolados continuam com create-drop e seed próprio; não substituem a integração MySQL.

## Verificação

Executar `sdk env` e `./mvnw verify`, com Docker ativo para Testcontainers. Conferir na primeira linha do `./mvnw -version` que o runtime é o 17: nesta máquina o `java` do PATH vem do shim do jenv, então o wrapper só usa o JDK correto quando `JAVA_HOME` está exportado pelo SDKMAN. O arquivo `.java-version` do projeto fixava 11, contradizendo `.sdkmanrc`; foi alinhado em 17 para que as duas ferramentas indiquem o mesmo runtime. Rodar o ciclo sem `JAVA_HOME` faz o build falhar em `build-info` com incompatibilidade de class file (55 contra 61) — o sintoma é de plugin, a causa é o JDK. A migração ajustou imports Jakarta, processamento de anotações e ordenação/identidades dos fixtures H2. Removidos somente stubs Mockito não utilizados; testes não foram desativados.

O profile WildFly legado exige servidor compatível com Jakarta e deve ser ensaiado separadamente. A implantação padrão proposta é o Docker executável; não realizar deploy automático durante os testes. Rollback de aplicação exige verificar compatibilidade de esquema e restaurar backup quando necessário, preservando evidências.
