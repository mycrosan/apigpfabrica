SELECT 
    c.id AS carcaca_id,
    c.numero_etiqueta AS etiqueta,
    p.id AS producao_id,
    r.id AS regra_id,
    r.status AS regra_status,
    CASE 
        WHEN r.status = 'VALIDADA' THEN 'Sim'
        WHEN r.status = 'EM_VALIDACAO' THEN 'Não - Em Validação'
        WHEN r.status = 'CANCELADA' THEN 'Não - Cancelada'
        ELSE 'Não - Status Desconhecido'
    END AS regra_validada,
    cq.id AS qualidade_id,
    tc.descricao AS classificacao_qualidade,
    tob.descricao AS observacao_qualidade,
    cq.observacao AS detalhes_qualidade,
    -- Informações adicionais úteis
    m.descricao AS modelo_descricao,
    md.descricao AS medida_descricao,
    p.medida_pneu_raspado,
    p.dt_create AS data_producao,
    c.dt_create AS data_carcaca_criacao
FROM carcaca c
LEFT JOIN producao p ON p.carcaca_id = c.id
LEFT JOIN regra r ON r.id = p.regra_id
LEFT JOIN controle_qualidade cq ON cq.producao_id = p.id
LEFT JOIN tipo_classificacao tc ON tc.id = cq.tipo_classificacao_id
LEFT JOIN tipo_observacao tob ON tob.id = cq.tipo_observacao_id
LEFT JOIN modelo m ON m.id = c.modelo_id
LEFT JOIN medida md ON md.id = c.medida_id
WHERE c.numero_etiqueta = ? -- Substitua ? pelo número da etiqueta desejada
ORDER BY p.dt_create DESC, cq.id DESC;

-- Versão alternativa com CASE para status mais legível
SELECT 
    c.id AS carcaca_id,
    c.numero_etiqueta AS etiqueta,
    COALESCE(p.id, 'Sem Produção') AS producao_id,
    COALESCE(r.id, 'Sem Regra') AS regra_id,
    COALESCE(r.status, 'N/A') AS regra_status,
    CASE 
        WHEN r.status = 'VALIDADA' THEN '✓ VALIDADA'
        WHEN r.status = 'EM_VALIDACAO' THEN '⚠ EM VALIDAÇÃO'
        WHEN r.status = 'CANCELADA' THEN '✗ CANCELADA'
        ELSE '• SEM STATUS'
    END AS regra_status_formatado,
    COALESCE(cq.id, 'Sem Qualidade') AS qualidade_id,
    COALESCE(tc.descricao, 'N/A') AS classificacao_qualidade,
    -- Informações da carcaça
    m.descricao AS modelo,
    md.descricao AS medida,
    p.medida_pneu_raspado,
    DATE_FORMAT(c.dt_create, '%d/%m/%Y %H:%i') AS data_criacao_carcaca,
    DATE_FORMAT(p.dt_create, '%d/%m/%Y %H:%i') AS data_producao
FROM carcaca c
LEFT JOIN producao p ON p.carcaca_id = c.id
LEFT JOIN regra r ON r.id = p.regra_id
LEFT JOIN controle_qualidade cq ON cq.producao_id = p.id
LEFT JOIN tipo_classificacao tc ON tc.id = cq.tipo_classificacao_id
LEFT JOIN modelo m ON m.id = c.modelo_id
LEFT JOIN medida md ON md.id = c.medida_id
WHERE c.numero_etiqueta = 027461
ORDER BY p.dt_create DESC;

-- Consulta resumida para verificar rapidamente o status
SELECT 
    c.numero_etiqueta AS etiqueta,
    CASE 
        WHEN p.id IS NULL THEN '❌ SEM PRODUÇÃO'
        WHEN r.status = 'VALIDADA' THEN '✅ PRODUZIDA - REGRA VALIDADA'
        WHEN r.status = 'EM_VALIDACAO' THEN '⚠️ PRODUZIDA - REGRA EM VALIDAÇÃO'
        WHEN r.status = 'CANCELADA' THEN '❌ PRODUZIDA - REGRA CANCELADA'
        ELSE '? PRODUZIDA - STATUS DESCONHECIDO'
    END AS status_geral,
    COALESCE(p.id, 0) AS producao_id,
    COALESCE(r.id, 0) AS regra_id,
    COALESCE(cq.id, 'N/A') AS qualidade_id
FROM carcaca c
LEFT JOIN producao p ON p.carcaca_id = c.id
LEFT JOIN regra r ON r.id = p.regra_id
LEFT JOIN controle_qualidade cq ON cq.producao_id = p.id
WHERE c.numero_etiqueta = 013478
LIMIT 1;