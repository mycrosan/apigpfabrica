# Orientações para agentes — API GP

## Escopo e referências

Aplicar a todo este repositório, especialmente Java, DTOs, controllers, serviços, persistência, configuração e testes. Respostas, explicações, comentários e documentação em português do Brasil. Ler também o AGENTS da raiz quando disponível.

Estas regras implementam as instruções do usuário de 05/09/2026. `CODING_STANDARDS.md`, `DEVELOPMENT.md` e `RULES_SUMMARY.md` são referências auxiliares; exemplos antigos de Java 11, entidades expostas, mutabilidade ou configuração não prevalecem sobre estas exigências.

Para reconhecimento de pneus, seguir a spec na issue central: <https://github.com/mycrosan/apigpfabrica/issues/1>. Em 15/09/2026, o usuário autorizou também visão por Ollama exclusivamente local, ampliando a exclusividade anterior de PaddleOCR. Ambos usam adaptadores no serviço Python interno, sem chamada externa de IA em nenhuma rota, comparação ou fallback. Ollama começa em avaliação e não herda aprovação do PaddleOCR. Falha encaminha ao cadastro manual com evidência. Consultar `ocr-local/OLLAMA.md` para provisionamento, compatibilidade e rollback.

## Ambiente e pré-requisitos

- Gerenciar Java com SDKMAN e fixar Java 17 LTS em `.sdkmanrc`, no build, CI e Docker. Não depender de jenv ou Java global para escolher o runtime deste projeto.
- Usar Maven Wrapper existente (`./mvnw`). Verificar versão efetiva do Java e Maven antes dos checks.
- Situação encontrada: Spring Boot 2.4.4, `<java.version>11</java.version>` e Bean Validation com `javax.validation`. A migração para Java 17 e uma stack compatível com `jakarta.validation` deve preceder os recursos que dependem dela.
- Não misturar anotações Jakarta com runtime que não as processe. Atualizar de forma coerente Spring, persistência, validação, segurança, documentação da API e dependências afetadas, com testes de regressão.
- MapStruct, Testcontainers e a execução efetiva do Flyway precisam ser configurados e verificados; a presença de arquivos de migração não comprova sua execução.
- Não alterar produção para preparar o ambiente de desenvolvimento. Preservar todas as alterações preexistentes no checkout.

## Arquitetura em camadas

- Todo fluxo de dados deve seguir **Controller → Service → Repository**. Controllers não acessam repositórios, EntityManager, SQL ou armazenamento diretamente.
- Controller valida o contrato HTTP, chama serviço e retorna DTO com status correto. Regras de negócio pertencem aos serviços.
- Repository encapsula consultas/persistência. Usar parâmetros; nunca concatenar entradas em SQL/JPQL.
- Serviços de aplicação podem coordenar adaptadores de OCR/arquivos por interfaces. Não forçar uma chamada a Repository em operações sem persistência apenas para cumprir a sequência.
- Preferir injeção por construtor e dependências `final`, usando `@RequiredArgsConstructor` quando apropriado.
- Aplicar SOLID, responsabilidade única, métodos curtos, nomes expressivos e baixa complexidade. Evitar serviços gigantes e utilitários sem domínio claro.

## DTOs, imutabilidade e MapStruct

- Sempre expor dados da API por DTOs; nunca serializar entidades JPA, proxies ou relacionamentos diretamente.
- Utilizar DTOs específicos de entrada e saída. Compatibilidade legada deve manter o formato planejado através de DTO/adaptador, sem perpetuar entidade como contrato.
- Preferir respostas imutáveis com records Java 17 ou Lombok `@Value`/`@Builder`. Usar `@Data` quando a mutabilidade for necessária, especialmente entradas; não aplicá-lo automaticamente a toda entidade.
- Atributos devem ser privados e acesso controlado. Não expor coleções mutáveis do estado interno; usar cópias defensivas/visões imutáveis.
- Utilizar MapStruct para mapear DTO ↔ entidade, integrado ao Spring. Configurar processamento de anotações em conjunto com Lombok e falha para campos de destino não tratados.
- Mapeamento não consulta banco nem decide regra de negócio. Serviço resolve relações e campos protegidos de auditoria; mapper não deve sobrescrevê-los a partir do cliente.
- Evitar `toString`, `equals` e `hashCode` gerados que percorram associações JPA ou exponham dados sensíveis.

## Validação e erros

- Aplicar **Jakarta Validation** nos DTOs (`jakarta.validation.constraints`) e `@Valid` nos controllers, incluindo objetos aninhados quando necessário.
- Validar também parâmetros de rota/consulta e limites de arquivos. Diferenciar formato inválido de regra de negócio.
- Centralizar erros com `@RestControllerAdvice`, DTO de erro estável e exceções customizadas de negócio/recurso/conflito. Reutilizar ou consolidar handlers existentes para evitar respostas concorrentes.
- Nunca retornar exceção como objeto de sucesso, converter falha em HTTP 200 nas rotas novas, usar catch genérico que continua o fluxo ou revelar stack trace ao cliente.
- REST: 201 em criação, 404 em ausência, 204 em deleção sem corpo; usar 400/403/409/413/422/503 conforme contrato da spec.
- Preservar a compatibilidade de consumidores antigos com adaptação planejada e testes de contrato durante a migração.
- Revalidar IDs, marca–modelo, medida, país, DOT e obrigatoriedade no salvamento e edição, mesmo quando informados manualmente.
- `confirmarCombinacaoNova` não contorna regras de integridade ou proibição. Histórico não comprova leitura visual correta.

## Transações e resiliência

- Marcar métodos públicos de escrita dos serviços com `@Transactional`; consultas ao banco com `@Transactional(readOnly = true)`.
- Aplicar transações a fronteiras reais de persistência. Não manter transação de banco aberta durante inferência OCR, upload remoto ou outras operações demoradas.
- Quando o caso de uso inclui I/O e persistência, separar etapas transacionais em serviços chamados pelo proxy Spring; não depender de autoinvocação de método anotado.
- Garantir atomicidade de valores finais, confirmações e vínculo da sessão; usar controle otimista para concorrência e idempotência para retries.
- Configurar timeout, tratamento de indisponibilidade e reconciliação de arquivos. Não repetir indefinidamente nem ocultar resultado incerto.
- Gerar usuário e horário de auditoria no servidor autenticado. Preservar tentativas e correções como eventos rastreáveis.

## Banco e Flyway

- Toda alteração de estrutura, índices, restrições ou dados de referência obrigatórios deve estar em script **Flyway versionado**.
- Não editar migração já aplicada. Criar versão subsequente e testar atualização a partir de esquema representativo existente e instalação nova.
- A configuração principal encontrada usa `ddl-auto=update`. Substituir o gerenciamento automático pelo Flyway antes de implantar mudanças de esquema.
- Em produção, nunca permitir que `ddl-auto` crie ou altere tabelas; usar `validate` ou `none` conforme configuração de implantação documentada.
- Planejar baseline para bancos existentes com inventário e validação; não habilitar baseline automático indiscriminadamente.
- Testes de integração devem executar as migrações reais no banco descartável. `create-drop` não substitui o teste do Flyway.
- Documentar compatibilidade de versões, backup/restauração e rollback de aplicação; não apagar dados para reverter um deploy.

## Configuração, logging e Docker

- Centralizar configurações em `application.yml` e perfis, com `@ConfigurationProperties` validado para grupos de parâmetros.
- Credenciais são referências a variáveis/segredos de implantação, nunca valores hardcoded nem segredos versionados no YAML. Migrar properties existentes evitando precedência ambígua.
- Externalizar endpoint/token interno do OCR, timeouts, limites, caminhos dos pesos, flags e políticas operacionais. Não codificar valores específicos de ambiente em classes.
- Usar SLF4J, preferencialmente `@Slf4j`, com logs parametrizados. Não usar `System.out`, `printStackTrace` ou logar base64, tokens e credenciais.
- Registrar eventos relevantes com sessão/tentativa/correlação, campo, versão e resultado técnico; evitar duplicar a mesma exceção em todas as camadas.
- Entregar Dockerfile otimizado com etapas de build/runtime, Java 17, dependências reproduzíveis e usuário sem privilégios quando compatível. Segredos não entram na imagem.
- PaddleOCR roda em serviço próprio interno; imagem/volume deve conter pesos versionados antes da execução, com readiness que comprove carregamento. Nenhuma chave de provedor externo deve ser necessária ao reconhecimento.

## Testes e definição de pronto

- Criar testes unitários com JUnit/Mockito para serviços, regras, validações e falhas relevantes.
- Criar integração com **Testcontainers**, usando banco compatível com produção e migrações Flyway reais. Não usar banco produtivo nos testes.
- Cobrir autorização, validação de DTO, status/erros HTTP, concorrência, idempotência, transações e rotas legadas afetadas.
- Configurar explicitamente a execução dos testes de integração, incluindo classes `*IT` quando usadas, no ciclo `verify`/CI; não assumir que Surefire já as executa.
- Após configurar SDKMAN/Java 17, executar `sdk env`, `./mvnw -version`, testes focados e `./mvnw verify` conforme o escopo. Docker deve estar disponível para Testcontainers.
- Executar checks de qualidade configurados; não desativá-los para obter build verde. Separar falhas legadas das introduzidas.
- Entregar DTOs/MapStruct, validação, erros, transações, migrações, configuração, documentação e testes coerentes com a mudança. Reportar verificações executadas e limitações reais.
