# PaddleOCR local para pneus

Serviço interno Python 3.11/PaddleOCR 3.3.2, sem API externa. Retorna texto, regiões e escore bruto; a API Java resolve catálogo e decide se apresenta sugestão. O escore não representa probabilidade calibrada.

## Preparação

Na pasta `ocr-local`, construir a imagem com `docker build -t fabrica-ocr:local .`. Executar `scripts/provisionar.py` com Python 3.11+ para baixar explicitamente os pesos públicos PP-OCRv5 mobile. O script grava `models/manifest.json` com URLs e checksums dos arquivos; conservar esse manifesto junto aos artefatos de cada implantação.

Configurar `OCR_TOKEN` como segredo do ambiente e executar `docker compose up -d --no-build --pull never` após construir a imagem local. O volume de modelos é somente leitura. A rede do motor é interna, sem saída para provedores. A ponte `acesso-local` encaminha somente para o motor e publica a porta de desenvolvimento em `127.0.0.1:8091`; isso permite acesso pela API no host mesmo quando o Docker não publica portas de redes internas. Ambos os serviços usam reinício `unless-stopped`.

Para desenvolvimento, criar `ocr-local/.env` (ignorado pelo Git, permissão 600) com `OCR_TOKEN`, `OCR_URL=http://127.0.0.1:8091` e `PNEUS_OCR_HABILITADO=true`. O Compose lê esse arquivo automaticamente; a API também o importa quando iniciada na raiz de `apigpfabrica`. Alterações exigem recarga/reinício da API. Sem configuração, o OCR permanece desabilitado. A lista de campos aprovados não é modificada: habilitar o motor não aprova sugestões para uso operacional.

Runtime não executa script de download. Pesos/token ausentes ou checksum inválido mantêm `/ready` em 503. `/health` verifica o processo. O aplicativo deve oferecer cadastro manual quando o OCR falhar.

## Contrato

Para testar diretamente no Postman, use a [coleção e o guia local](postman/README.md).
O pacote inclui ambiente sem credenciais, imagem sintética e conversor local
de foto para o JSON exigido pelo endpoint.

POST `/v1/reconhecer`, `Authorization: Bearer <token interno>`, JSON com `campo` (MARCA/MODELO/MEDIDA/PAIS/DOT) e `foto_base64`.

Resposta: `motor`, `versaoBiblioteca`, `versaoModelo`, `versaoPreprocessamento`, `campo`, `linhas` (`texto`, `escore`, `regiao`), `duracaoMs`. Sem IDs de catálogo. 400 para imagem/contrato inválido, 403 para autorização, 413 para limite, 503 para motor indisponível/ocupado. O token e o conteúdo das imagens não são registrados em logs.

## DOT em relevo — perfil de 11/09/2026

`OCR_PERFIL_DOT=dot-relevo-v1` é o padrão. Para DOT, executa a passagem original e uma passagem adicional em escala de cinza (OpenCV BGR → cinza → BGR), mantendo as dimensões. Na passagem adicional, o detector usa lado máximo de 960 pixels, limiar de pixels 0,15, limiar de caixas 0,3 e expansão de caixas 2,0. O Paddle remapeia as regiões para a imagem de entrada. OpenCV 4.10.0.84 já está fixado no lockfile.

As linhas das duas passagens são preservadas. O motor não junta caracteres isolados, não completa datas e não escolhe entre valores conflitantes. A API continua responsável por validar o DOT, resolver candidatos e exigir confirmação; nenhuma aprovação de campo foi alterada. Demais campos continuam com uma passagem. O custo da passagem adicional deve ser medido no conjunto de avaliação.

`versaoModelo` incorpora o manifesto dos pesos e o perfil/parâmetros de execução. `versaoPreprocessamento` informa `exif-transpose-rgb-dot-relevo-v1` para DOT; a API registra esse valor no snapshot. Respostas de servidores antigos, sem esse campo adicional, mantêm o identificador legado na API. O identificador composto muda mesmo em campos sem a passagem adicional, pois identifica a configuração do motor.

Validação local: uma foto real de 3072×4080, sem recorte manual, passou de nenhuma linha no perfil original a uma sequência de quatro dígitos no perfil adicional. O endpoint também foi testado com cópia JPEG qualidade 95, nas mesmas dimensões, em aproximadamente 5,5 segundos. O PNG original de aproximadamente 13 MiB ultrapassa o limite HTTP configurado; o limite de 8 MiB foi preservado. Artefato local: `fabrica/dados-ocr-local/ensaio-dot-relevo-20260911/resultado.json`. Uma foto não mede acurácia; a proposta permanece sem rótulo aprovado e não é elegível para treinamento.

Para aplicar, reconstruir a imagem e recriar o serviço `ocr`. Reiniciar a API recompilada para persistir a versão adicional. Rollback: configurar `OCR_PERFIL_DOT=legado` no ambiente do Compose e recriar `ocr`, restaurando uma passagem e o hash original do manifesto. Não há alteração de esquema nem treinamento de pesos nesta mudança.

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

Reproduzir com `docker compose up -d --no-build --pull never` e um cliente HTTP em `http://127.0.0.1:8091`; a ponte expõe somente o acesso local, preservando o isolamento do motor.

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
