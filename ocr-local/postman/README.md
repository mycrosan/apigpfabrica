# Testar o PaddleOCR local no Postman

Esta coleção acessa diretamente o serviço Python em `http://127.0.0.1:8091`.
Não depende de login no aplicativo nem da API Java. O resultado é a leitura
bruta: não resolve IDs de catálogo, não valida semana/ano do DOT e não aprova
sugestões para preenchimento automático.

## Importar e configurar

1. No Postman Desktop, importe `OCR_Local.postman_collection.json` e
   `OCR_Local.postman_environment.json` desta pasta.
2. Selecione o ambiente **OCR local — desenvolvimento**.
3. Mantenha `ocr_base_url` como `http://127.0.0.1:8091`.
4. Preencha `ocr_token` com o valor de `OCR_TOKEN` do arquivo `ocr-local/.env`,
   sem incluir `Bearer`. É o token interno do OCR, não o JWT do aplicativo.
5. Use armazenamento local, sem sincronizar token, fotos, histórico de respostas
   ou transcrições com serviços externos. Os arquivos distribuídos não contêm
   o token real nem fotos da fábrica. A coleção não grava respostas em variáveis.

O Postman precisa estar no mesmo computador do serviço para usar `127.0.0.1`.
Se necessário, inicie os serviços na pasta `ocr-local` com:

```sh
docker compose up -d --no-build --pull never
```

## Executar os testes

Execute as requisições na ordem:

| Requisição | Resultado esperado |
|---|---|
| 01 — Saúde do processo | HTTP 200, `status: ativo` |
| 02 — Modelos prontos | HTTP 200, `status: pronto` e versão dos modelos |
| 03 — Reconhecer DOT sintético | HTTP 200 e uma ou mais linhas de texto |
| 04 — Reconhecer minha foto | HTTP 200; confira `linhas[].texto` |
| 05 — Conferir bloqueio sem token | HTTP 403; esse erro é esperado neste teste |

A terceira requisição já inclui a imagem sintética `DOT 3625`, sem precisar
selecionar arquivo. Ela verifica o funcionamento do motor; não comprova a
precisão em fotos reais de pneus. A aba **Test Results** mostra as verificações
automáticas. A quarta requisição precisa de um arquivo preparado abaixo e deve
ser executada individualmente depois de selecioná-lo.

## Enviar sua foto

O endpoint atual aceita JSON, não upload direto de JPEG nem `form-data`.
Na pasta desta coleção, execute:

```sh
python3 preparar_foto.py "/caminho/da/foto.jpg" --campo DOT
```

O conversor apenas lê a foto localmente e cria um JSON protegido na pasta
`requisicoes`, ignorada pelo Git. Ele mostra o caminho gerado e não envia nada.
Na requisição **04**, abra **Body → binary → Select File**, selecione esse
arquivo **JSON** e clique em **Send**. Mantenha `Content-Type: application/json`.

Para outro campo, use `--campo MARCA`, `MODELO`, `MEDIDA` ou `PAIS` ao preparar
a foto. JPEG, PNG e WEBP são aceitos, até 8 MiB de arquivo e 20 megapixels.
O servidor valida o conteúdo e as dimensões; o conversor verifica tamanho e extensão.

Se aparecer `Operation not permitted` ao abrir a foto em Downloads, autorize
o aplicativo que hospeda seu terminal (Terminal, iTerm ou editor) em
**Ajustes do Sistema → Privacidade e Segurança → Arquivos e Pastas → Downloads**.
Reabra o aplicativo e execute novamente o comando. O conversor não altera
permissões do macOS. [Orientação da Apple](https://support.apple.com/pt-br/guide/mac-help/mchld27a5c7a/mac).

## Interpretar a resposta

- `linhas[].texto`: texto extraído pelo OCR. Confira se os quatro dígitos do
  DOT aparecem corretamente aqui antes de investigar a validação da API Java.
- `linhas[].escore`: escore bruto do motor, não probabilidade de acerto.
- `linhas[].regiao`: coordenadas da região reconhecida na imagem.
- `duracaoMs`: tempo da inferência, sem incluir toda a transferência.
- HTTP 200 com `linhas: []`: processamento concluído sem texto reconhecido.
- HTTP 400: confira o JSON, o campo e a imagem; a resposta traz `detail`.
- HTTP 403: token ausente ou diferente do configurado no serviço.
- HTTP 413: imagem acima dos limites.
- HTTP 503: motor sem modelos, ocupado ou indisponível; confira `/ready`.
- Conexão recusada: o endereço não está acessível ou o serviço está parado.

Para reenviar a mesma foto, clique novamente em **Send**. Execute os envios
em sequência: o serviço aceita uma inferência por vez.
