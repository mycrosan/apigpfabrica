# Depuração da leitura por campo

Cada foto enviada representa somente o campo selecionado: MARCA, MODELO, MEDIDA, PAIS ou DOT. Uma sessão pode reunir vários campos; o identificador de leitura representa uma requisição, não um pneu inteiro.

Após reiniciar a API e recompilar o app, veja `leitura_pneu` no console de depuração do Flutter. Copie o `id` do evento `envio` e procure esse mesmo `id` nos logs INFO da API. A correlação é transportada no cabeçalho `X-Leitura-Id`, devolvida pela API e encaminhada ao OCR interno.

Sequência esperada:

1. App: `envio`, campo e destino.
2. API: `leitura recebida` e `campo_recebido`, com contexto de marca/modelo.
3. API: `ocr envio`, com campo, host, porta e timeout.
4. API: `ocr chamada_encerrada` registra duração, inclusive em falha; não significa sucesso.
5. API: `ocr retorno_validado`, com quantidade de linhas e duração do motor.
6. API: `leitura decisao`, com estado, motivos e quantidade de candidatos.
7. API: `resposta_app`, com status HTTP; app: `retorno`, com status e duração.

`nao_enviado` informa OCR desabilitado ou token ausente; `circuito_aberto` evita chamada após falhas; `falha` informa tipo técnico e status HTTP quando disponível; `contrato_invalido` indica resposta incompatível, inclusive campo diferente do solicitado. `MODELO_NAO_APROVADO` na decisão significa que houve leitura, mas as sugestões daquele campo ainda não foram liberadas.

No fluxo v2, `tentativa_preparada` liga a correlação à sessão e tentativa. `tentativa_reutilizada` indica idempotência: a API reutilizou o resultado sem nova inferência.

Os logs não incluem fotos, base64, transcrições, tokens ou corpos HTTP. O serviço Python recebe o cabeçalho de correlação; esta mudança registra a comunicação com ele no lado Java, sem alterar seus logs internos. Nenhuma confirmação automática de campo foi adicionada.

## Comparar a imagem do aplicativo no Postman

O evento `ocr corpo_preparado` registra tamanho em bytes, comprimento do base64 e MD5 da imagem (apenas para comparação de arquivos, não para segurança).
Para exportar o corpo completo enviado ao OCR, inicie a API com `OCR_DIAGNOSTICO_SALVAR_JSON=true`. Opcionalmente configure `OCR_DIAGNOSTICO_DIRETORIO` com um diretório privado local, fora de pastas públicas ou sincronizadas. O padrão é `diagnostico-ocr` no diretório de execução da API, ignorado pelo Git neste repositório.

Faça uma nova leitura e procure `ocr json_salvo` pelo mesmo `id`. O campo `arquivo` mostra o caminho absoluto do JSON com `campo` e `foto_base64`, sem token. No Postman, selecione esse arquivo em Body → binary, mantendo Content-Type application/json e a autenticação local existente. Assim a imagem enviada é exatamente a encaminhada pela API, sem outra captura ou recompressão.

Arquivos novos usam permissão 600 e diretórios novos 700 em sistemas POSIX. Falha na exportação produz `json_nao_salvo`, sem interromper OCR. A exportação fica desativada por padrão; desligue a variável após o diagnóstico e remova os arquivos manualmente quando concluir (não há limpeza automática). Eles contêm a foto completa e não devem ser compartilhados com serviços externos.
