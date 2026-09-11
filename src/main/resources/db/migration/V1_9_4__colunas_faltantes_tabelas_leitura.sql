-- Ajusta colunas faltantes nas tabelas de leitura criadas pela V1_9_3.
-- As migrations originais V1_8.0/V1_9.0 foram criadas anteriormente e nao possuem
-- algumas colunas adicionadas posteriormente nas entidades JPA.

DELIMITER $$
DROP PROCEDURE IF EXISTS add_col_if_not_exists $$
CREATE PROCEDURE add_col_if_not_exists(
    IN p_table VARCHAR(128), IN p_col VARCHAR(128), IN p_def TEXT
)
BEGIN
    DECLARE existe INT DEFAULT 0;
    SELECT COUNT(*) INTO existe
      FROM information_schema.columns c
     WHERE c.table_schema = DATABASE() AND c.table_name = p_table AND c.column_name = p_col;
    IF existe = 0 THEN
        SET @__ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_col, '` ', p_def);
        PREPARE stmt FROM @__ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$
DELIMITER ;

-- ===== leitura_imagem (3 colunas) =====
CALL add_col_if_not_exists('leitura_imagem', 'mime',        'VARCHAR(32) NULL');
CALL add_col_if_not_exists('leitura_imagem', 'largura',     'INT NULL');
CALL add_col_if_not_exists('leitura_imagem', 'altura',      'INT NULL');

-- ===== leitura_sessao (5 colunas) =====
CALL add_col_if_not_exists('leitura_sessao', 'carcaca_inicial_id',        'INT NULL');
CALL add_col_if_not_exists('leitura_sessao', 'motivo_combinacao_nova',    'VARCHAR(1024) NULL');
CALL add_col_if_not_exists('leitura_sessao', 'motivo_abandono',           'VARCHAR(1024) NULL');
CALL add_col_if_not_exists('leitura_sessao', 'abandonada_em',             'DATETIME(6) NULL');
CALL add_col_if_not_exists('leitura_sessao', 'abandonada_por',            'INT NULL');

DROP PROCEDURE IF EXISTS add_col_if_not_exists;
