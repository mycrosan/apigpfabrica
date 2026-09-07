# Preparação local das fotos reais

As fotos ficam no diretório de origem, sem cópia, remoção ou envio a provedores. Os scripts geram artefatos locais. Não adicionar fotos, dumps, transcrições ou arquivos SQLite ao GitHub.

## Inventário

Usar Python 3.9+ com Pillow 11.2.1. O motor usa o ambiente fixado no Dockerfile/requirements.lock.

```sh
.venv/bin/python scripts/inventariar_fotos.py --origem /caminho/fotos --destino /caminho/dados/inventario
```

Gera SQLite, manifesto JSONL e resumo. Verifica SHA-256, assinatura/decodificação, limites da API, dimensões, presença de GPS e hash visual para triagem. **Decodificável não significa texto legível.** Cópias exatas contam uma vez no manifesto; os originais permanecem intactos. Fotos parecidas não são eliminadas pelo hash visual, pois um dígito diferente pode ser relevante.

## Vínculos com cadastros

```sh
.venv/bin/python scripts/vincular_cadastros_legados.py --dump /caminho/backup.sql --destino /caminho/dados/inventario
.venv/bin/python scripts/consolidar_corpus.py --diretorio /caminho/dados/inventario
```

O parser lê somente a estrutura e INSERTs da tabela `carcaca`; não executa SQL. Exporta apenas vínculos entre nomes de imagens e carcaças, com proveniência/hash do dump. Valores de cadastro não viram transcrições. Conflitos e ausência de vínculo são explícitos. Conferir o grupo físico antes de congelar partições; não usar DOT como identificador único.

Executar a consolidação somente após finalizar o inventário. Não iniciar treino enquanto houver somente registros `LEGADO_NAO_REVISADO`.

## Teste do serviço com fotos reais

`avaliar_amostra_local.py` roda dentro da imagem Docker, com `/fotos` e `/models` montados somente para leitura e `/saida` para resultados. Usar `--network none`, sistema de arquivos somente leitura e `/tmp` temporário. O script gera token efêmero interno, inicia o serviço, verifica readiness e escolhe uma foto por pasta `*/carcaca`, de forma determinística e sem repetir hash exato.

Produz `leituras_nao_revisadas.jsonl`, `revisao_cega.csv` e resumo de operação. A planilha cega não contém o texto sugerido; transcrição, campo, legibilidade, grupo e revisor ficam vazios. A região é uma proposta automática que também precisa ser conferida.

Resposta HTTP 200, escore alto, quantidade de texto ou latência **não medem acurácia**. Sem transcrição humana independente, não existe métrica de acerto nem amostra aprovada para treino. A seleção de uma foto por pasta é teste de funcionamento, não conjunto representativo de avaliação.

## Antes do treinamento

1. Conferir a foto/região e transcrever literalmente, sem consultar a sugestão do OCR primeiro.
2. Marcar imagens ilegíveis sem inventar texto a partir do cadastro.
3. Registrar revisão independente e segunda revisão nos casos definidos pela spec.
4. Conferir grupos físicos e conflitos entre fotos/cadastros.
5. Exportar somente regiões/transcrições aprovadas; separar grupos inteiros em treino, validação e teste e congelar o teste.
6. Comparar baseline e candidato no mesmo conjunto revisado. Os scripts desta pasta não promovem modelos nem habilitam sugestões.
