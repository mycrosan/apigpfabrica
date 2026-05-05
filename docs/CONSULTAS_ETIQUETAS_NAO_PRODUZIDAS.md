# Consultas SQL: Etiquetas não produzidas e medida sugerida pela regra

Este documento reúne consultas SQL para:
- Listar etiquetas (carcaças) que ainda **não possuem produção**;
- Exibir a **medida** (descrição) definida pela **regra** aplicável;
- Trazer os limites de tamanho (tamanho_min, tamanho_max) e uma **medida sugerida** para `medida_pneu_raspado`.

As consultas consideram as seguintes tabelas conforme o projeto:
- `carcaca (id, numero_etiqueta, modelo_id, medida_id, pais_id, status, ...)`
- `producao (id, carcaca_id, medida_pneu_raspado, regra_id, ...)`
- `regra (id, modelo_id, medida_id, pais_id, tamanho_min, tamanho_max, status, dt_create, ...)`
- `medida (id, descricao)`

Observação: se seu domínio exige apenas **regras validadas**, mantenha o filtro `r.status = 'VALIDADA'` nas consultas.

---

## 1) Consulta simples (uma regra por combinação)

Retorna carcaças sem produção e a medida da regra relacionada pela combinação modelo/medida/país.

```sql
SELECT
  c.id AS carcaca_id,
  c.numero_etiqueta AS etiqueta,
  md.descricao AS medida_regra,
  r.tamanho_min AS tamanho_min_permitido,
  r.tamanho_max AS tamanho_max_permitido,
  ((r.tamanho_min + r.tamanho_max) / 2) AS medida_pneu_raspado_sugerida,
  r.id AS regra_id
FROM carcaca c
JOIN regra r
  ON r.modelo_id = c.modelo_id
 AND r.medida_id = c.medida_id
 AND r.pais_id   = c.pais_id
LEFT JOIN medida md ON md.id = r.medida_id
WHERE NOT EXISTS (
  SELECT 1 FROM producao p WHERE p.carcaca_id = c.id
)
-- opcional: apenas regras validadas
AND (r.status = 'VALIDADA')
-- opcional: apenas carcaças em status inicial
-- AND c.status = 'start'
ORDER BY c.numero_etiqueta;
```

Quando existem múltiplas regras para a mesma combinação (modelo, medida, país), use uma das próximas opções para escolher a mais recente.

---

## 2) Consulta avançada (Window Function – regra mais recente VALIDADA)

Escolhe a **regra mais recente** por combinação usando `ROW_NUMBER()` (MySQL 8+, H2 recente, PostgreSQL).

```sql
WITH regras_validas AS (
  SELECT
    r.*,
    ROW_NUMBER() OVER (
      PARTITION BY r.modelo_id, r.medida_id, r.pais_id
      ORDER BY r.dt_create DESC, r.id DESC
    ) AS rn
  FROM regra r
  WHERE r.status = 'VALIDADA'
)
SELECT
  c.id AS carcaca_id,
  c.numero_etiqueta AS etiqueta,
  md.descricao AS medida_regra,
  rv.tamanho_min AS tamanho_min_permitido,
  rv.tamanho_max AS tamanho_max_permitido,
  ((rv.tamanho_min + rv.tamanho_max) / 2) AS medida_pneu_raspado_sugerida,
  rv.id AS regra_id
FROM carcaca c
JOIN regras_validas rv
  ON rv.modelo_id = c.modelo_id
 AND rv.medida_id = c.medida_id
 AND rv.pais_id   = c.pais_id
 AND rv.rn        = 1
LEFT JOIN medida md ON md.id = rv.medida_id
WHERE NOT EXISTS (
  SELECT 1 FROM producao p WHERE p.carcaca_id = c.id
)
-- opcional
-- AND c.status = 'start'
ORDER BY c.numero_etiqueta;
```

---

## 3) Consulta sem Window Function (correlated subquery – regra mais recente VALIDADA)

Alternativa compatível com bancos que não suportam window functions. Seleciona a versão mais recente evitando outra regra da mesma combinação com `dt_create` (ou `id`) maior.

```sql
SELECT
  c.id AS carcaca_id,
  c.numero_etiqueta AS etiqueta,
  md.descricao AS medida_regra,
  r.tamanho_min AS tamanho_min_permitido,
  r.tamanho_max AS tamanho_max_permitido,
  ((r.tamanho_min + r.tamanho_max) / 2) AS medida_pneu_raspado_sugerida,
  r.id AS regra_id
FROM carcaca c
JOIN regra r
  ON r.modelo_id = c.modelo_id
 AND r.medida_id = c.medida_id
 AND r.pais_id   = c.pais_id
LEFT JOIN medida md ON md.id = r.medida_id
WHERE r.status = 'VALIDADA'
  AND NOT EXISTS (
    SELECT 1
    FROM regra r2
    WHERE r2.status = 'VALIDADA'
      AND r2.modelo_id = r.modelo_id
      AND r2.medida_id = r.medida_id
      AND r2.pais_id   = r.pais_id
      AND (
        r2.dt_create > r.dt_create OR
        (r2.dt_create = r.dt_create AND r2.id > r.id)
      )
  )
  AND NOT EXISTS (
    SELECT 1 FROM producao p WHERE p.carcaca_id = c.id
  )
-- opcional
-- AND c.status = 'start'
ORDER BY c.numero_etiqueta;
```

---

## Observações
- A coluna `medida_pneu_raspado_sugerida` é apenas uma referência (média entre `tamanho_min` e `tamanho_max`). Use as regras de negócio reais para definir o valor final.
- Se houver múltiplas regras por combinação, prefira as consultas 2 ou 3 para evitar duplicidades.
- Ajuste nomes de colunas/tabelas se houver diferenças entre ambientes (PRD vs H2 de testes).
- Em bases antigas, pode ser útil adicionar `AND c.status = 'start'` para filtrar carcaças explicitamente não iniciadas.

---

## Validação rápida
- Verifique se existem carcaças sem produção: `SELECT COUNT(*) FROM carcaca c WHERE NOT EXISTS (SELECT 1 FROM producao p WHERE p.carcaca_id = c.id);`
- Verifique se existem regras VALIDADAS: `SELECT COUNT(*) FROM regra r WHERE r.status = 'VALIDADA';`
- Caso não existam regras validadas, as consultas podem retornar vazio.

---

## Anexo: Consultas MySQL com 4 casas decimais sem arredondamento

Estas versões são específicas para MySQL e asseguram que `tamanho_min`, `tamanho_max` e a `medida_pneu_raspado_sugerida` sejam exibidos com 4 casas decimais SEM arredondamento, usando `TRUNCATE` e `CAST(... AS DECIMAL(20,4))` para manter zeros à direita.

Compatibilidade:
- Window Functions e CTEs (`WITH`) requerem MySQL 8.0+.
- Para MySQL 5.7, utilize a consulta sem Window Function.

### 1) MySQL — Consulta simples (uma regra por combinação)

```sql
SELECT
  c.id AS carcaca_id,
  c.numero_etiqueta AS etiqueta,
  md.descricao AS medida_regra,
  CAST(TRUNCATE(r.tamanho_min, 4) AS DECIMAL(20,4)) AS tamanho_min_permitido,
  CAST(TRUNCATE(r.tamanho_max, 4) AS DECIMAL(20,4)) AS tamanho_max_permitido,
  CAST(TRUNCATE((r.tamanho_min + r.tamanho_max) / 2, 4) AS DECIMAL(20,4)) AS medida_pneu_raspado_sugerida,
  r.id AS regra_id
FROM carcaca c
JOIN regra r
  ON r.modelo_id = c.modelo_id
 AND r.medida_id = c.medida_id
 AND r.pais_id   = c.pais_id
LEFT JOIN medida md ON md.id = r.medida_id
WHERE NOT EXISTS (
  SELECT 1 FROM producao p WHERE p.carcaca_id = c.id
)
-- opcional: apenas regras validadas
-- AND r.status = 'VALIDADA'
ORDER BY c.numero_etiqueta, r.id;
```

### 2) MySQL 8+ — Consulta avançada (Window Function: regra mais recente VALIDADA)

```sql
WITH regras_validas AS (
  SELECT
    r.*,
    ROW_NUMBER() OVER (
      PARTITION BY r.modelo_id, r.medida_id, r.pais_id
      ORDER BY r.dt_create DESC, r.id DESC
    ) AS rn
  FROM regra r
  WHERE r.status = 'VALIDADA'
)
SELECT
  c.id AS carcaca_id,
  c.numero_etiqueta AS etiqueta,
  md.descricao AS medida_regra,
  CAST(TRUNCATE(rv.tamanho_min, 4) AS DECIMAL(20,4)) AS tamanho_min_permitido,
  CAST(TRUNCATE(rv.tamanho_max, 4) AS DECIMAL(20,4)) AS tamanho_max_permitido,
  CAST(TRUNCATE((rv.tamanho_min + rv.tamanho_max) / 2, 4) AS DECIMAL(20,4)) AS medida_pneu_raspado_sugerida,
  rv.id AS regra_id
FROM carcaca c
JOIN regras_validas rv
  ON rv.modelo_id = c.modelo_id
 AND rv.medida_id = c.medida_id
 AND rv.pais_id   = c.pais_id
 AND rv.rn        = 1
LEFT JOIN medida md ON md.id = rv.medida_id
WHERE NOT EXISTS (
  SELECT 1 FROM producao p WHERE p.carcaca_id = c.id
)
-- opcional
-- AND c.status = 'start'
ORDER BY c.numero_etiqueta, rv.id;
```

### 3) MySQL 5.7 — Consulta sem Window Function (regra mais recente VALIDADA)

```sql
SELECT
  c.id AS carcaca_id,
  c.numero_etiqueta AS etiqueta,
  md.descricao AS medida_regra,
  CAST(TRUNCATE(r.tamanho_min, 4) AS DECIMAL(20,4)) AS tamanho_min_permitido,
  CAST(TRUNCATE(r.tamanho_max, 4) AS DECIMAL(20,4)) AS tamanho_max_permitido,
  CAST(TRUNCATE((r.tamanho_min + r.tamanho_max) / 2, 4) AS DECIMAL(20,4)) AS medida_pneu_raspado_sugerida,
  r.id AS regra_id
FROM carcaca c
JOIN regra r
  ON r.modelo_id = c.modelo_id
 AND r.medida_id = c.medida_id
 AND r.pais_id   = c.pais_id
LEFT JOIN medida md ON md.id = r.medida_id
WHERE r.status = 'VALIDADA'
  AND NOT EXISTS (
    SELECT 1
    FROM regra r2
    WHERE r2.status = 'VALIDADA'
      AND r2.modelo_id = r.modelo_id
      AND r2.medida_id = r.medida_id
      AND r2.pais_id   = r.pais_id
      AND (
        r2.dt_create > r.dt_create OR
        (r2.dt_create = r.dt_create AND r2.id > r.id)
      )
  )
  AND NOT EXISTS (
    SELECT 1 FROM producao p WHERE p.carcaca_id = c.id
  )
-- opcional
-- AND c.status = 'start'
ORDER BY c.numero_etiqueta, r.id;
```

### Dicas de diagnóstico
- Se `tamanho_min` e `tamanho_max` aparecerem iguais, valide os dados: `SELECT id, tamanho_min, tamanho_max FROM regra WHERE tamanho_min = tamanho_max;`
- Para garantir zeros à direita, use sempre `CAST(... AS DECIMAL(20,4))` após `TRUNCATE`.
- Se precisar integrar com H2 ou Postgres, substitua `TRUNCATE(x, 4)` por a técnica com `FLOOR(x * 10000) / 10000` e ajuste o tipo para `NUMERIC(20,4)`.



SELECT
c.id AS carcaca_id,
c.numero_etiqueta AS etiqueta,
md.descricao AS medida_regra,
CAST(TRUNCATE(r.tamanho_min, 3) AS DECIMAL(20,3)) AS tamanho_min_permitido,
CAST(TRUNCATE(r.tamanho_max, 3) AS DECIMAL(20,3)) AS tamanho_max_permitido,
CAST(TRUNCATE((r.tamanho_min + r.tamanho_max) / 2, 3) AS DECIMAL(20,3)) AS medida_pneu_raspado_sugerida,
r.id AS regra_id
FROM carcaca c
JOIN regra r
ON r.modelo_id = c.modelo_id
AND r.medida_id = c.medida_id
AND r.pais_id   = c.pais_id
LEFT JOIN medida md ON md.id = r.medida_id
WHERE NOT EXISTS (
SELECT 1 FROM producao p WHERE p.carcaca_id = c.id
)
-- opcional: somente regras validadas
-- AND r.status = 'VALIDADA'
ORDER BY r.id;