# Implementação da leitura local de pneus

Atualização: 05/09/2026. Branch local `codex/paddleocr-local`. Implementação em andamento, sem implantação em produção.

## O que está implementado

- Java 17 via SDKMAN, Spring Boot 3/Jakarta, DTOs/MapStruct, validação central e Flyway real em MySQL descartável.
- PaddleOCR em serviço Python interno, com pesos locais, manifesto de checksums, autenticação, readiness e execução offline após provisionamento.
- API v2 de abertura/consulta de sessão, foto/tentativa, confirmação e criação/edição técnica. Operador e horários são definidos no servidor.
- Idempotência de abertura, tentativa e cadastro; conflito quando a chave é reutilizada com outro conteúdo. A abertura pode ser reenviada após o vínculo da carcaça.
- Gravação da imagem e tentativa antes da inferência, em transação separada. Falhas de OCR preservam a evidência e retornam HTTP 503 com identificador.
- Fotos JPEG/PNG verificadas por decodificação, tamanho, dimensões, MIME e SHA-256. Escrita por arquivo temporário e publicação atômica; leitura rejeita links simbólicos e alteração do checksum.
- Extração original, regiões, versão dos pesos/biblioteca, snapshot do catálogo, hash e política de decisão preservados por execução, inclusive quando a leitura falha: indisponibilidade do OCR guarda catálogo, política e identificação sem extração; falha de resolução guarda também a extração já obtida. Confirmações são eventos separados.
- Cada execução registra a versão do artefato e o resumo da configuração de decisão vigente, para distinguir erro de leitura de mudança de política ou de versão ao reler a evidência.
- Circuito de proteção do OCR: após falhas consecutivas as chamadas são recusadas de imediato durante a espera configurada, liberando uma sonda por vez em seguida. Evita somar o timeout à espera de cada operador e mantém o caminho manual como saída. O estado é por instância da API, não distribuído.
- Abandono explícito de sessão em `POST /api/v2/leitura-sessoes/{id}/abandono`, com motivo obrigatório e versão vigente. Encerrar não apaga evidência: fotos, tentativas e confirmações continuam auditáveis, e a sessão passa a recusar nova tentativa, confirmação ou cadastro. Autor e horário são do servidor; o reenvio do mesmo abandono é idempotente.
- Troca de marca/modelo/medida invalida confirmações dependentes. Valores finais são reconstruídos das confirmações no servidor. Motivo da combinação inédita é persistido.
- Catálogo restrito ao campo e contexto em `GET /api/v2/leitura-catalogo/{campo}`; resposta por DTO.
- Rotina opcional recupera tentativas em `PROCESSANDO` há mais de cinco minutos como `RESULTADO_INCERTO`. Resultado tardio não substitui essa decisão. Arquivos sem vínculo com mais de 24 horas podem ser reconciliados.

## Contratos de uso

1. `POST /api/v2/leitura-sessoes`, `Idempotency-Key: <UUID>`, corpo `{}` para novo cadastro ou `{"carcacaId": 123}` para edição.
2. `POST /api/v2/leitura-sessoes/{id}/tentativas`, chave persistida pelo cliente durante retry, corpo com `campo`, `foto_base64`, `marcaIdContexto`, `modeloIdContexto`, `versaoSessao`.
3. `GET /api/v2/leitura-sessoes/{id}` recupera o histórico e a versão vigente. Cada tentativa retorna `resultado` aninhado; esse é o contrato implementado consumido pelo aplicativo.
4. `POST /api/v2/leitura-sessoes/{id}/confirmacoes` informa campo, tentativa quando disponível, origem, valor/ID, motivo e versão. Não aprova a imagem para treinamento.
5. `POST /api/v2/carcacas` ou `PUT /api/v2/carcacas/{id}` informa sessão, versão, etiqueta e justificativa de combinação inédita quando necessária. A API exige os cinco campos confirmados e revalida as regras.

As imagens ficam em `/api/v2/leitura-imagens/{id}`, com autenticação e acesso do operador proprietário. Outra pessoa não obtém acesso por conhecer o UUID.

## Configuração e execução

Usar `application.yml`, variáveis de ambiente e as orientações de `MIGRACAO_JAVA17.md`. Não aplicar baseline automaticamente em banco existente.

- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`: configuração do ambiente.
- `OCR_URL`, `OCR_TOKEN`: serviço local descrito em `../ocr-local/README.md`.
- `PNEUS_OCR_HABILITADO=true`: permite executar a leitura local. O padrão é desabilitado.
- `PNEUS_OCR_CAMPOS_APROVADOS`: lista de campos com aprovação experimental. O padrão é vazio; não habilitar apenas para obter preenchimento automático.
- `EVIDENCIAS_DIRETORIO`: volume persistente da API.
- `EVIDENCIAS_RECONCILIACAO=true`: habilita a rotina de recuperação/reconciliação. Padrão desabilitado; ela não aplica política de retenção nem exclui evidências vinculadas.
- `pneus.ocr.falhas-para-abrir` (padrão 3) e `pneus.ocr.abertura-segundos` (padrão 30): limite de falhas consecutivas e espera do circuito de proteção. Valores altos demais mantêm operadores esperando o timeout; baixos demais recusam leituras por instabilidade passageira.

A rotina de reconciliação deve operar junto à API que tem acesso ao volume de evidências. Não é política completa de exclusão/retensão. O relógio/intervalo máximo de inferência devem permanecer compatíveis com a janela de recuperação.

## Verificação e limites

`source ~/.sdkman/bin/sdkman-init.sh`, `sdk env`, `./mvnw -B clean verify`. O ciclo executa testes unitários/Mockito e integrações Testcontainers, com as migrações Flyway reais. Usar Docker disponível. A compilação limpa evita reaproveitar classes geradas pela IDE durante a migração de namespaces.

O teste do motor sem rede leu uma imagem **sintética** com `205/55R16`. Isso verifica provisionamento e inferência local, não precisão em pneus. Fotos reais revisadas ainda são necessárias.

## Pendências antes de encerrar as issues

- **API #4:** falta retenção por status e derivados, expurgo auditado, remoção de geolocalização sem perder orientação útil e reconciliação de temporários deixados por interrupção de processo. Confirmar a política operacional antes do piloto. O fluxo explícito de abandono e retomada foi implementado e testado em 05/09/2026.
- **API #5:** provisionamento concluído em 06/09/2026 com teste HTTP real de 46 fotos, sem rede. Resultados em `../ocr-local/README.md`. Isso não aprova precisão nem promoção do modelo.
- **API #6:** falta o teste ponta a ponta com o serviço PaddleOCR real em execução; a verificação atual usa o cliente isolado e o serviço substituído por dublê. Cobertura do resolvedor de catálogo/DOT, proteção de circuito, snapshot de execução em falhas e identificação de build/configuração foram implementados e verificados em 05/09/2026.
- **App #2:** falta validar câmera/retomada em aparelho, persistir localmente a captura ainda não recebida pelo servidor, oferecer busca na seleção de catálogo e conferir regressões do aplicativo migrado. As chaves de fotos pendentes permanecem em memória; somente a sessão é persistida no armazenamento seguro. O consumo da flag por campo foi implementado em 05/09/2026: o motivo `MODELO_NAO_APROVADO` marca a leitura como em sombra, o aplicativo avisa o operador e nunca oferece confirmação de sugestão nesse estado.
- **API #7 / App #3:** fila de revisão independente, permissões, transcrição cega, segunda revisão e adjudicação ainda não implementadas.
- **API #8–#11:** exportação versionada, partições por pneu, avaliação, treinamento especializado e piloto/promoção ainda não concluídos. Nenhuma confirmação operacional é usada para treinamento.

As issues permanecem em andamento quando os critérios acima ainda não foram cumpridos. O código desta etapa está no checkout local; não foi publicado nem implantado automaticamente.
