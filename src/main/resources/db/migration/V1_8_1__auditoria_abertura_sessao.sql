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

CALL add_col_if_not_exists('leitura_sessao', 'carcaca_inicial_id',        'INT NULL');
CALL add_col_if_not_exists('leitura_sessao', 'motivo_combinacao_nova',    'VARCHAR(1024) NULL');

UPDATE leitura_sessao SET carcaca_inicial_id = carcaca_id WHERE status = 'ABERTA';

DROP PROCEDURE IF EXISTS add_col_if_not_exists;
