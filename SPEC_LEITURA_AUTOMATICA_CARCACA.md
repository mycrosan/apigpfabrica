# SPEC — Leitura Automática da Carcaça por Câmera + IA

> **Status:** proposta aprovada para priorização · **Data:** 16/07/2026
> **Problema que resolve:** cadastros errados de marca/modelo/medida/DOT/país na classificação da carcaça (4 erros só em 15/07), que contaminam todo o fluxo downstream (compra → inspeção → produção → venda).

---

## 1. Objetivo

No ato do cadastro/classificação técnica da carcaça no app de produção, o operador **fotografa a lateral do pneu** e o sistema lê e pré-preenche automaticamente:

| Campo | Origem na lateral do pneu |
|---|---|
| **Marca** | Logotipo/nome em relevo (ex.: MICHELIN, PIRELLI) |
| **Modelo** | Nome comercial (ex.: X MULTI D, FG85) |
| **Medida** | Dimensão (ex.: 295/80R22.5) |
| **DOT (4 dígitos)** | Final do código DOT = semana + ano de fabricação (ex.: `2323` = 23ª semana de 2023) |
| **País** | "MADE IN ..." ou código de fábrica do DOT |

O operador **apenas confere** os campos sugeridos (destacados por nível de confiança) e confirma. **O sistema nunca cadastra sozinho** — a confirmação humana é obrigatória; campo ilegível vem vazio e destacado em vermelho para digitação manual.

Bônus: a foto da lateral fica **arquivada como evidência** no campo `fotos` da carcaça (`CarcacaModel.fotos`, que existe hoje e não é usado no cadastro).

---

## 2. Por que é um projeto rápido

A infraestrutura já existe no GPControl — só falta a "cabeça" de IA:

| Peça | Situação |
|---|---|
| Endpoint de classificação `POST /api/carcaca/{id}/cadastro-tecnico` (`modeloId`, `medidaId`, `paisId`, `dot` 4 dígitos) | ✅ Pronto no backend (ainda nem é consumido pelo app) |
| Catálogos com ID + descrição (`/api/marca`, `/api/modelo`, `/api/medida`, `/api/pais`) | ✅ Prontos |
| Envio de foto base64 e armazenamento (local/Firebase) | ✅ Padrão já usado no fluxo de compra (`CompraFluxoService.processarFotoCompra`) |
| Câmera no app (Expo) | ✅ Já usada no scanner e na foto da compra |
| Leitura por IA de visão | ❌ **É o que este projeto adiciona** |

---

## 3. Arquitetura da solução

### Fase 1 — MVP por software (sem cabine; celular do operador)

```
Operador (app produção)
   │ 1. etapa CLASSIFICAR → botão "📷 Ler carcaça"
   │ 2. foto da lateral (expo-image-picker, base64)
   ▼
POST /api/carcaca/{id}/leitura-lateral        ← novo endpoint
   │ 3. LeituraCarcacaService:                ← novo serviço
   │    • monta prompt com os catálogos do banco (id + descrição)
   │    • chama a API do Claude (visão) com a foto
   │    • structured outputs → JSON garantido com IDs do catálogo
   │    • salva a foto (FileStorageService) e grava URL em carcaca.fotos
   ▼
Resposta: { modeloId, medidaId, paisId, dot, textos lidos, confiança por campo }
   │ 4. app pré-preenche os dropdowns e destaca confiança baixa
   │ 5. operador confere/corrige → confirma
   ▼
POST /api/carcaca/{id}/cadastro-tecnico       ← endpoint existente
```

**Contrato do novo endpoint (proposta):**

```json
// POST /api/carcaca/{id}/leitura-lateral
// request
{ "foto_base64": "data:image/jpeg;base64,..." }

// response
{
  "modeloId": 12,   "modeloTexto": "X MULTI D",
  "marcaId": 3,     "marcaTexto": "MICHELIN",
  "medidaId": 7,    "medidaTexto": "295/80R22.5",
  "paisId": 2,      "paisTexto": "BRASIL",
  "dot": "2323",    "dotCompleto": "DOT B94W 00RX 2323",
  "confianca": { "marca": "ALTA", "modelo": "MEDIA", "medida": "ALTA", "dot": "BAIXA", "pais": "ALTA" },
  "fotoUrl": "carcacas_leitura/2026000123_ab12.jpg"
}
```

**IA de visão:** API do Claude, modelo `claude-opus-4-8` (suporta imagens em alta resolução — até 2576 px no lado maior — importante para o relevo preto-sobre-preto da lateral), via SDK Java oficial (`com.anthropic:anthropic-java`). O retorno usa **structured outputs** (JSON Schema) — a resposta vem sempre no formato esperado, sem parsing frágil. Os catálogos vão no prompt com **prompt caching** (custam ~10% nas chamadas seguintes). Se o texto lido não bater com nenhum item do catálogo, o campo vem sem ID (só o texto lido) para o operador decidir — inclusive é assim que se descobre marca/modelo novo que falta cadastrar.

**Segurança/config:** chave `ANTHROPIC_API_KEY` como variável de ambiente no backend (mesmo padrão adotado para `DB_USERNAME`/`DB_PASSWORD`).

### Fase 2 — Cabine de leitura (hardware)

A fase 1 já funciona com o celular na mão, mas a **cabine padroniza enquadramento e luz**, que é o que leva a leitura do DOT de "às vezes" para "quase sempre":

```
        ┌──────────────────────────────┐
        │      [celular fixo, zenital] │  ← suporte articulado, câmera para baixo
        │                              │
        │  barra LED ↘          ↙ barra LED   ← luz RASANTE (~20–30°), lados opostos
        │                              │
        │        ( pneu deitado )      │
        │   fundo fosco escuro (EVA/   │
        │   borracha preta fosca)      │
        └──────────────────────────────┘
```

- **Iluminação é o fator nº 1.** O texto da lateral é relevo preto sobre preto: luz frontal/flash "apaga" o relevo; **luz rasante em ângulo baixo cria sombras que revelam o texto**. Duas barras de LED opostas + difusor (~R$ 200–400). Nada de flash do celular.
- **Fundo fosco escuro** (tapete de borracha/EVA preto) elimina reflexos e facilita o recorte.
- O operador gira o pneu até a região do DOT ficar visível e dispara pelo próprio app.

### Qual câmera comprar? → um celular Android dedicado (recomendado)

| Opção | Custo | Prós | Contras |
|---|---|---|---|
| **✅ Celular Android dedicado fixo no suporte, rodando o app GP** (ex.: Moto G84, Galaxy A35/A55 — ou reaproveitar um aparelho bom que já tenham) | R$ 0–1.900 + suporte ~R$ 100 | Zero integração extra (o fluxo já é o app); câmera superior a webcams da mesma faixa; foco automático; troca fácil | Precisa de tomada/carregador fixo |
| Webcam USB 4K (Logitech Brio 4K) + mini-PC | ~R$ 2.000–2.500 + PC | Estação fixa "parruda" | Exige desenvolver uma captura web/desktop que não existe → mais dias de projeto. **Não recomendada para a v1** |

> Resposta direta à pergunta "qual câmera comprar": **não compre webcam** — compre (ou reaproveite) um **celular Android intermediário** e invista a diferença na **iluminação**, que importa mais que a câmera.

---

## 4. Precisão esperada e mitigação de erros

- **Marca e medida:** alta taxa de acerto (texto grande e padronizado).
- **Modelo:** média-alta; o matching contra o catálogo (lista fechada) elimina "quase acertos".
- **DOT:** o mais difícil (relevo baixo, às vezes gasto). Melhora muito com a luz rasante da cabine. Enquanto ilegível, o operador digita só os 4 dígitos — ainda assim com a foto arquivada como evidência.
- **Confiança por campo:** o app destaca visualmente o que precisa de atenção (verde = confere rápido, amarelo = olhe, vermelho = digite).
- **Melhoria contínua:** logar divergências (o que a IA sugeriu × o que o operador confirmou). Esse log mede a acurácia real por campo e alimenta a calibração do prompt — e diz com dados quando dá para confiar mais.

---

## 5. Prazo

| Etapa | Duração (dias úteis) |
|---|---|
| Backend: endpoint + `LeituraCarcacaService` + prompt/matching + salvar foto | 2–3 |
| Frontend: captura, services de marca/modelo/país (hoje só existe o de medida), tela de conferência, integração com `cadastro-tecnico` | 2–3 |
| Calibração com fotos reais do pátio + testes com operadores | 1–2 |
| **MVP total (fase 1)** | **~5–8** |
| Cabine (fase 2): compra + montagem em paralelo; ajuste fino de luz/enquadramento | +1–2 após montada |

---

## 6. Custos

**Por leitura (API do Claude, Opus 4.8 — US$ 5/M tokens de entrada, US$ 25/M de saída):**

| Item | Tokens (aprox.) |
|---|---|
| Foto da lateral | 1.600–4.800 |
| Catálogos no prompt (com cache ≈ 10% do preço) | 2.000–5.000 → efetivos ~200–500 |
| JSON de resposta | ~200 |

≈ **US$ 0,015–0,03 por carcaça (~R$ 0,08–0,17)**. Para ~500 carcaças/mês: **~R$ 40–85/mês**.
Após medir a acurácia, dá para testar `claude-haiku-4-5` (~5× mais barato) e decidir com dados.

**Hardware (fase 2):**

| Item | Custo |
|---|---|
| Celular Android intermediário (ou reaproveitado) | R$ 0–1.900 |
| Suporte articulado de mesa/teto | ~R$ 100 |
| 2 barras LED + difusor + fita para ângulo rasante | R$ 200–400 |
| Tapete de borracha/EVA preto fosco (fundo) | ~R$ 50 |
| **Total** | **R$ 350–2.450** |

**Pré-requisito:** criar conta/chave na API da Anthropic (`ANTHROPIC_API_KEY`).

---

## 7. Fora de escopo da v1

- Treinamento de modelo de visão próprio (desnecessário — o matching contra catálogo fechado resolve).
- Leitura em movimento/esteira.
- Cadastro 100% automático sem conferência humana.

## 8. Próximos passos

1. ✅ Aprovar este documento (prazo, câmera e orçamento).
2. Criar a chave da API Anthropic e definir `ANTHROPIC_API_KEY` no backend.
3. Implementar fase 1 (MVP por software) — branch `feat/leitura-carcaca`.
4. Em paralelo: comprar suporte + iluminação (+ celular, se não houver aparelho para reaproveitar) e montar a cabine.
5. Rodar 1 semana em paralelo com o processo atual medindo o log de divergências; calibrar e expandir.
