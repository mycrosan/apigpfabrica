# PaddleOCR local para pneus

Serviço interno Python 3.11/PaddleOCR 3.3.2, sem API externa. Retorna texto, regiões e escore bruto; a API Java resolve catálogo e decide se apresenta sugestão. O escore não representa probabilidade calibrada.

## Preparação

Na pasta `ocr-local`, construir a imagem com `docker build -t fabrica-ocr:local .`. Executar `scripts/provisionar.py` com Python 3.11+ para baixar explicitamente os pesos públicos PP-OCRv5 mobile. O script grava `models/manifest.json` com URLs e checksums dos arquivos; conservar esse manifesto junto aos artefatos de cada implantação.

Configurar `OCR_TOKEN` como segredo do ambiente e executar `docker compose up -d`. O volume de modelos é somente leitura. A rede do Compose é interna, sem saída para provedores. Conectar a API à mesma rede na implantação; a porta local de desenvolvimento é limitada a 127.0.0.1.

Runtime não executa script de download. Pesos/token ausentes ou checksum inválido mantêm `/ready` em 503. `/health` verifica o processo. O aplicativo deve oferecer cadastro manual quando o OCR falhar.

## Contrato

POST `/v1/reconhecer`, `Authorization: Bearer <token interno>`, JSON com `campo` (MARCA/MODELO/MEDIDA/PAIS/DOT) e `foto_base64`.

Resposta: `motor`, `versaoBiblioteca`, `versaoModelo` (hash do manifesto), `campo`, `linhas` (`texto`, `escore`, `regiao`), `duracaoMs`. Sem IDs de catálogo. 400 para imagem/contrato inválido, 403 para autorização, 413 para limite, 503 para motor indisponível/ocupado. O token e o conteúdo das imagens não são registrados em logs.

Limites padrão: 8 MiB decodificados e 20 megapixels, configuráveis por `OCR_MAX_BYTES`/`OCR_MAX_PIXELS`. Um processo e uma inferência simultânea por instância para limitar memória; a API controla timeout e tentativas.

## Recursos medidos

Medição de 05/09/2026, imagem `fabrica-ocr:local` sob Docker Desktop em Apple Silicon (arm64), um processo uvicorn, uma inferência por vez, pesos PP-OCRv5 mobile montados em `/models` somente leitura.

| Medida | Valor |
|---|---|
| Subida até `/ready` responder `pronto` | ~5 s após o start do contêiner |
| Memória residente em repouso | 260 MiB |
| Memória residente após 13 inferências | 340 MiB |
| Primeira inferência (inclui aquecimento) | 290 ms |
| Inferência aquecida — mediana / p95 / máx (parede, n=12) | 211 / 230 / 242 ms |
| Inferência aquecida — mediana / p95 / máx (`duracaoMs` do motor) | 208 / 227 / 238 ms |

Entrada da medição: JPEG sintético de 900×300, 7,5 KB, com `205/55R16 91V MADE IN BRAZIL DOT 3625`. O motor devolveu a linha completa com escore bruto 0,973.

**Estes números não medem precisão.** A imagem é sintética, com tipografia limpa e sem curvatura, borracha, sujeira ou sombra — condições que dominam a lateral de um pneu real. Servem para dimensionar contêiner e timeout, não para decidir aprovação de campo. O `pneus.ocr.timeout-segundos` padrão de 15 s tem folga larga sobre o p95 medido justamente porque fotos reais são maiores e mais custosas.

Reproduzir com `docker compose up -d` e um cliente HTTP dentro do contêiner; a rede do Compose é interna e a porta não fica exposta ao host.

## Verificação

Testes de contrato usam `pytest` e `httpx`. A validação do motor deve incluir início e inferência com `--network none`, pesos montados e imagem conhecida.

Verificado em 05/09/2026: contêiner iniciado com `--network none` e volume de modelos **vazio** responde `/health` 200 `{"status":"ativo"}` e `/ready` **503** `{"detail":"OCR não provisionado"}`, sem nenhuma tentativa de download nos logs — a única URL registrada é o banner do uvicorn. Com os pesos montados, a inferência ocorre normalmente sem saída de rede. Testes sintéticos e contrato não comprovam precisão em pneus; o piloto exige fotos reais revisadas, conforme issue central.

Referência dos caminhos locais e configuração: https://www.paddleocr.ai/main/en/version3.x/pipeline_usage/OCR.html

## Teste com fotos reais — 06/09/2026

Executado `scripts/avaliar_amostra_local.py` na imagem reconstruída a partir do lockfile, com `--network none`, fotos/pesos somente para leitura, token efêmero local e uma foto de cada uma das 46 pastas fornecidas. O teste usa o endpoint HTTP real, não um dublê do motor.

- Ambiente: Apple M4 Pro; Docker com 12 CPUs disponíveis e aproximadamente 7,65 GiB de memória. Um processo OCR; inventário de imagens em quatro threads concorria por recursos no host.
- Readiness: 3,01 segundos. Manifesto dos pesos: `d16031c014cec216c1eb29b89b44cf8dc60ad4670d247bd420b16bff429b1274`.
- 46/46 requisições HTTP 200; 45 fotos com algum texto extraído; 509 regiões propostas.
- Latência de parede p50: 6.587 ms; p95: 11.763 ms. A carga concorrente impede tratar essa medição como benchmark isolado do ambiente piloto. O p95 observado supera a meta inicial de 10 segundos e deverá ser avaliado na fase de desempenho.
- **Acurácia não medida.** Nenhuma transcrição aprovada, nenhum campo/modelo promovido. Extração de texto não comprova acerto de marca, medida ou DOT.

Resultados completos são locais, em `fabrica/dados-ocr-local/amostra-real-20260906/`. `leituras_nao_revisadas.jsonl` conserva propostas do OCR; `revisao_cega.csv` não revela essas transcrições e pode iniciar a conferência humana. Fotos e transcrições não são publicadas no GitHub.
