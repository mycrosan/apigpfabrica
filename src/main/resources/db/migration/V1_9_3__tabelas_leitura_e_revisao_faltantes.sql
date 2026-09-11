-- Cria tabelas faltantes em schema legado (baseline < 1.9.0) que sao referenciadas
-- por entidades JPA mas nao existiam em 'fabrica'.
-- Fontes: V1_8_0 (leitura_sessao, leitura_imagem, leitura_tentativa, leitura_confirmacao),
--         V1_8_2 (leitura_execucao), V1_9_0 (revisao_imagem, revisao_item, revisao_resposta).

CREATE TABLE IF NOT EXISTS leitura_sessao (
  id CHAR(36) PRIMARY KEY, operador_id INT NOT NULL, carcaca_id INT NULL,
  grupo_fisico CHAR(36) NOT NULL, status VARCHAR(20) NOT NULL,
  criada_em DATETIME(6) NOT NULL, versao BIGINT NOT NULL DEFAULT 0,
  confirmacoes_json LONGTEXT NOT NULL,
  CONSTRAINT fk_leitura_sessao_operador FOREIGN KEY (operador_id) REFERENCES usuario(id),
  CONSTRAINT fk_leitura_sessao_carcaca FOREIGN KEY (carcaca_id) REFERENCES carcaca(id),
  INDEX ix_leitura_sessao_operador (operador_id, status, criada_em)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS leitura_imagem (
  id CHAR(36) PRIMARY KEY, sessao_id CHAR(36) NOT NULL, arquivo VARCHAR(255) NOT NULL,
  sha256 CHAR(64) NOT NULL, tamanho_bytes BIGINT NOT NULL, recebida_em DATETIME(6) NOT NULL,
  CONSTRAINT fk_leitura_imagem_sessao FOREIGN KEY (sessao_id) REFERENCES leitura_sessao(id),
  INDEX ix_leitura_imagem_hash (sha256)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS leitura_tentativa (
  id CHAR(36) PRIMARY KEY, sessao_id CHAR(36) NOT NULL, imagem_id CHAR(36) NOT NULL,
  chave_idempotencia VARCHAR(128) NOT NULL, hash_requisicao CHAR(64) NOT NULL,
  campo VARCHAR(10) NOT NULL, contexto_json LONGTEXT NOT NULL, versao_sessao BIGINT NOT NULL,
  estado VARCHAR(30) NOT NULL, resposta_json LONGTEXT NULL, criada_em DATETIME(6) NOT NULL,
  CONSTRAINT fk_leitura_tentativa_sessao FOREIGN KEY (sessao_id) REFERENCES leitura_sessao(id),
  CONSTRAINT fk_leitura_tentativa_imagem FOREIGN KEY (imagem_id) REFERENCES leitura_imagem(id),
  CONSTRAINT uq_leitura_tentativa_idempotencia UNIQUE (sessao_id, chave_idempotencia),
  INDEX ix_leitura_tentativa_fila (estado, campo, criada_em)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS leitura_confirmacao (
  id CHAR(36) PRIMARY KEY, sessao_id CHAR(36) NOT NULL, tentativa_id CHAR(36) NULL,
  campo VARCHAR(10) NOT NULL, origem VARCHAR(20) NOT NULL, item_final_id INT NULL,
  valor_final VARCHAR(255) NULL, motivo VARCHAR(1024) NULL, operador_id INT NOT NULL,
  criada_em DATETIME(6) NOT NULL, versao_sessao BIGINT NOT NULL,
  CONSTRAINT fk_leitura_confirmacao_sessao FOREIGN KEY (sessao_id) REFERENCES leitura_sessao(id),
  CONSTRAINT fk_leitura_confirmacao_tentativa FOREIGN KEY (tentativa_id) REFERENCES leitura_tentativa(id),
  CONSTRAINT fk_leitura_confirmacao_operador FOREIGN KEY (operador_id) REFERENCES usuario(id),
  INDEX ix_leitura_confirmacao_campo (sessao_id, campo, versao_sessao)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS leitura_execucao (
  tentativa_id CHAR(36) PRIMARY KEY,
  snapshot_json LONGTEXT NOT NULL,
  hash_catalogo CHAR(64) NOT NULL,
  criada_em DATETIME(6) NOT NULL,
  CONSTRAINT fk_leitura_execucao_tentativa FOREIGN KEY (tentativa_id) REFERENCES leitura_tentativa(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS revisao_imagem (
  id CHAR(36) PRIMARY KEY, sha256 CHAR(64) NOT NULL UNIQUE,
  arquivo VARCHAR(255) NOT NULL, previa VARCHAR(255) NOT NULL, hash_previa CHAR(64) NOT NULL,
  largura INT NOT NULL, altura INT NOT NULL, criada_em DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS revisao_item (
  id CHAR(36) PRIMARY KEY, chave_origem CHAR(64) NOT NULL UNIQUE,
  imagem_id CHAR(36) NOT NULL, origem VARCHAR(24) NOT NULL,
  sessao_id CHAR(36), tentativa_id CHAR(36), confirmacao_id CHAR(36), operador_id INT,
  campo_solicitado VARCHAR(10), marca_id INT, modelo_id INT,
  regiao_json LONGTEXT NOT NULL, evidencia_json LONGTEXT NOT NULL,
  valor_cadastro VARCHAR(255), correcao BIT NOT NULL, ambiguidade BIT NOT NULL,
  estado VARCHAR(20) NOT NULL, reavaliar BIT NOT NULL, pendencia_cadastro BIT NOT NULL,
  ciclo INT NOT NULL, ultima_resposta_id CHAR(36), versao BIGINT NOT NULL, criada_em DATETIME(6) NOT NULL,
  CONSTRAINT fk_revisao_imagem FOREIGN KEY (imagem_id) REFERENCES revisao_imagem(id),
  CONSTRAINT fk_revisao_sessao FOREIGN KEY (sessao_id) REFERENCES leitura_sessao(id),
  CONSTRAINT fk_revisao_tentativa FOREIGN KEY (tentativa_id) REFERENCES leitura_tentativa(id),
  CONSTRAINT fk_revisao_confirmacao FOREIGN KEY (confirmacao_id) REFERENCES leitura_confirmacao(id),
  CONSTRAINT fk_revisao_operador FOREIGN KEY (operador_id) REFERENCES usuario(id),
  INDEX idx_revisao_fila (estado, criada_em), INDEX idx_revisao_contexto (marca_id, modelo_id),
  INDEX idx_revisao_sessao (sessao_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS revisao_resposta (
  id CHAR(36) PRIMARY KEY, item_id CHAR(36) NOT NULL, revisor_id INT NOT NULL,
  chave CHAR(36) NOT NULL, hash_requisicao CHAR(64) NOT NULL,
  ciclo INT NOT NULL, campo VARCHAR(10), transcricao VARCHAR(512),
  legibilidade VARCHAR(24) NOT NULL, regiao_json LONGTEXT NOT NULL,
  adjudicacao BIT NOT NULL, motivo VARCHAR(1024), criada_em DATETIME(6) NOT NULL,
  CONSTRAINT fk_resposta_item FOREIGN KEY (item_id) REFERENCES revisao_item(id),
  CONSTRAINT fk_resposta_revisor FOREIGN KEY (revisor_id) REFERENCES usuario(id),
  UNIQUE KEY uk_revisao_revisor (item_id, revisor_id, ciclo),
  UNIQUE KEY uk_revisao_chave (revisor_id, chave)
) ENGINE=InnoDB;

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

CALL add_col_if_not_exists('leitura_sessao', 'chave_cadastro', 'VARCHAR(128) NULL');
CALL add_col_if_not_exists('leitura_sessao', 'hash_cadastro',  'CHAR(64) NULL');

DROP PROCEDURE IF EXISTS add_col_if_not_exists;

INSERT INTO perfil (descricao) SELECT 'REVISAR_LEITURA'
 WHERE NOT EXISTS (SELECT 1 FROM perfil WHERE descricao = 'REVISAR_LEITURA');
INSERT INTO perfil (descricao) SELECT 'IMPORTAR_LEITURA'
 WHERE NOT EXISTS (SELECT 1 FROM perfil WHERE descricao = 'IMPORTAR_LEITURA');
