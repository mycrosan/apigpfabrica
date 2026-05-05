SELECT 
    c.numero_etiqueta,
    p.dt_create AS data_remodelagem,
    md.descricao AS medida,
    m.descricao AS marca,
    ml.descricao AS modelo,
    mz.descricao AS matriz,
    r.id AS regra_utilizada,
    p.medida_pneu_raspado AS tamanho_raspado,
    cb.descricao AS camelback_utilizado,
    a1.descricao as antiquebra1,
    a2.descricao as antiquebra2,
    a3.descricao as antiquebra3,
    e.descricao as espessuramento,
    r.tempo
FROM
    gppremium.producao p
        JOIN
    carcaca c ON c.id = p.carcaca_id
        JOIN
    medida md ON md.id = c.medida_id
        JOIN
    modelo ml ON ml.id = c.modelo_id
        JOIN
    marca m ON m.id = ml.marca_id
        JOIN
    regra r ON r.id = p.regra_id
        JOIN
    matriz mz ON mz.id = r.matriz_id
    JOIN
    camelback cb on cb.id = r.camelback_id
    JOIN
    antiquebra a1 on a1.id = r.antiquebra1_id
    JOIN
    antiquebra a2 on a2.id = r.antiquebra2_id
    JOIN
    antiquebra a3 on a3.id = r.antiquebra3_id
    JOIN 
    espessuramento e on e.id = r.espessuramento_id
