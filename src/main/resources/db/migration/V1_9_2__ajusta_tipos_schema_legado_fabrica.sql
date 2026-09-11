-- Ajusta tipos de colunas em bancos legados (ex: fabrica) para bater com as entidades JPA.
-- Colunas que usam @Convert(JpaConverterJson) sao convertidas para TEXT (JSONs podem exceder 255 chars).
-- Colunas de data sao normalizadas para DATETIME(6) (compativel com java.util.Date + precision 6).
-- Todas as operacoes sao idempotentes via checagem no information_schema.

DELIMITER $$

DROP PROCEDURE IF EXISTS add_col_if_not_exists $$
CREATE PROCEDURE add_col_if_not_exists(
    IN p_table VARCHAR(128), IN p_col VARCHAR(128), IN p_def TEXT
)
BEGIN
    DECLARE col_count INT DEFAULT 0;
    SELECT COUNT(*) INTO col_count FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_col;
    IF col_count = 0 THEN
        SET @__ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_col, '` ', p_def);
        PREPARE stmt FROM @__ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END $$

DROP PROCEDURE IF EXISTS modify_col_if_diff $$
CREATE PROCEDURE modify_col_if_diff(
    IN p_table VARCHAR(128), IN p_col VARCHAR(128), IN p_def TEXT
)
BEGIN
    DECLARE col_count INT DEFAULT 0;
    SELECT COUNT(*) INTO col_count FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_col;
    IF col_count > 0 THEN
        SET @__ddl = CONCAT('ALTER TABLE `', p_table, '` MODIFY COLUMN `', p_col, '` ', p_def);
        PREPARE stmt FROM @__ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END $$

DELIMITER ;

-- ===== Colunas novas ausentes em schema legado =====
CALL add_col_if_not_exists('carcaca', 'leitura_metadados', 'TEXT NULL');

-- ===== Colunas JSON / TEXT =====
CALL modify_col_if_diff('carcaca', 'dados', 'TEXT');
CALL modify_col_if_diff('carcaca', 'fotos', 'TEXT');
CALL modify_col_if_diff('producao', 'dados', 'TEXT');
CALL modify_col_if_diff('producao', 'fotos', 'TEXT');
CALL modify_col_if_diff('controle_qualidade', 'fotos', 'TEXT');
CALL modify_col_if_diff('valida_regra', 'dados', 'TEXT');

-- ===== Colunas VARCHAR =====
CALL modify_col_if_diff('antiquebra', 'descricao', 'VARCHAR(255)');
CALL modify_col_if_diff('auditoria', 'registro_id', 'INT NOT NULL');
CALL modify_col_if_diff('camelback', 'descricao', 'VARCHAR(255)');
CALL modify_col_if_diff('carcaca', 'dot', 'VARCHAR(255)');
CALL modify_col_if_diff('carcaca', 'numero_etiqueta', 'VARCHAR(255)');
CALL modify_col_if_diff('carcaca', 'status', 'VARCHAR(255)');
CALL modify_col_if_diff('carcaca_rejeitada', 'descricao', 'VARCHAR(255)');
CALL modify_col_if_diff('carcaca_rejeitada', 'motivo', 'VARCHAR(255)');
CALL modify_col_if_diff('controle_qualidade', 'observacao', 'VARCHAR(255)');
CALL modify_col_if_diff('espessuramento', 'descricao', 'VARCHAR(255)');
CALL modify_col_if_diff('marca', 'descricao', 'VARCHAR(255)');
CALL modify_col_if_diff('matriz', 'descricao', 'VARCHAR(255)');
CALL modify_col_if_diff('medida', 'descricao', 'VARCHAR(255)');
CALL modify_col_if_diff('modelo', 'descricao', 'VARCHAR(255)');
CALL modify_col_if_diff('pais', 'descricao', 'VARCHAR(255)');
CALL modify_col_if_diff('perfil', 'descricao', 'VARCHAR(255)');
CALL modify_col_if_diff('regra', 'tempo', 'VARCHAR(255)');
CALL modify_col_if_diff('status_carcaca', 'descricao', 'VARCHAR(255)');
CALL modify_col_if_diff('tipo_classificacao', 'descricao', 'VARCHAR(255)');
CALL modify_col_if_diff('tipo_observacao', 'descricao', 'VARCHAR(255)');
CALL modify_col_if_diff('usuario', 'login', 'VARCHAR(255) NOT NULL');
CALL modify_col_if_diff('usuario', 'nome', 'VARCHAR(255) NOT NULL');
CALL modify_col_if_diff('usuario', 'senha', 'VARCHAR(255) NOT NULL');

-- ===== Colunas DATETIME(6) =====
CALL modify_col_if_diff('carcaca', 'dt_create', 'DATETIME(6)');
CALL modify_col_if_diff('carcaca', 'dt_update', 'DATETIME(6)');
CALL modify_col_if_diff('carcaca_rejeitada', 'dt_create', 'DATETIME(6)');
CALL modify_col_if_diff('carcaca_rejeitada', 'dt_update', 'DATETIME(6)');
CALL modify_col_if_diff('cobertura', 'dt_create', 'DATETIME(6)');
CALL modify_col_if_diff('cobertura', 'dt_update', 'DATETIME(6)');
CALL modify_col_if_diff('cola', 'data_inicio', 'DATETIME(6) NOT NULL');
CALL modify_col_if_diff('controle_qualidade', 'dt_create', 'DATETIME(6)');
CALL modify_col_if_diff('maquina_configuracao', 'dt_create', 'DATETIME(6) NOT NULL');
CALL modify_col_if_diff('maquina_registro', 'dt_create', 'DATETIME(6) NOT NULL');
CALL modify_col_if_diff('producao', 'dt_create', 'DATETIME(6)');
CALL modify_col_if_diff('producao', 'dt_update', 'DATETIME(6)');
CALL modify_col_if_diff('regra', 'dt_create', 'DATETIME(6)');
CALL modify_col_if_diff('regra', 'dt_delete', 'DATETIME(6)');
CALL modify_col_if_diff('regra', 'dt_update', 'DATETIME(6)');

-- ===== DOUBLE =====
CALL modify_col_if_diff('producao', 'medida_pneu_raspado', 'DOUBLE');
CALL modify_col_if_diff('regra', 'tamanho_max', 'DOUBLE');
CALL modify_col_if_diff('regra', 'tamanho_min', 'DOUBLE');

-- ===== ENUMs =====
CALL modify_col_if_diff('cola', 'status', 'ENUM(''Aguardando'',''Pronto'',''Vencido'') NOT NULL');
CALL modify_col_if_diff('maquina_registro', 'status', 'ENUM(''Ativa'',''Inativa'',''Manutencao'') NOT NULL');
CALL modify_col_if_diff('pneus_vulcanizados', 'status', 'ENUM(''FINALIZADO'',''INICIADO'') NOT NULL');
CALL modify_col_if_diff('regra', 'status', 'ENUM(''EM_VALIDACAO'',''VALIDADA'')');

-- ===== Limpeza =====
DROP PROCEDURE IF EXISTS add_col_if_not_exists;
DROP PROCEDURE IF EXISTS modify_col_if_diff;
