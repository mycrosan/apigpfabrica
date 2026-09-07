CREATE TABLE leitura_sessao (
 id CHAR(36) PRIMARY KEY, operador_id INT NOT NULL, carcaca_id INT NULL,
 grupo_fisico CHAR(36) NOT NULL, status VARCHAR(20) NOT NULL,
 criada_em DATETIME(6) NOT NULL, versao BIGINT NOT NULL DEFAULT 0,
 confirmacoes_json LONGTEXT NOT NULL,
 CONSTRAINT fk_leitura_sessao_operador FOREIGN KEY (operador_id) REFERENCES usuario(id),
 CONSTRAINT fk_leitura_sessao_carcaca FOREIGN KEY (carcaca_id) REFERENCES carcaca(id),
 INDEX ix_leitura_sessao_operador (operador_id, status, criada_em)
) ENGINE=InnoDB;
CREATE TABLE leitura_imagem (
 id CHAR(36) PRIMARY KEY, sessao_id CHAR(36) NOT NULL, arquivo VARCHAR(255) NOT NULL,
 sha256 CHAR(64) NOT NULL, tamanho_bytes BIGINT NOT NULL, recebida_em DATETIME(6) NOT NULL,
 CONSTRAINT fk_leitura_imagem_sessao FOREIGN KEY (sessao_id) REFERENCES leitura_sessao(id),
 INDEX ix_leitura_imagem_hash (sha256)
) ENGINE=InnoDB;
CREATE TABLE leitura_tentativa (
 id CHAR(36) PRIMARY KEY, sessao_id CHAR(36) NOT NULL, imagem_id CHAR(36) NOT NULL,
 chave_idempotencia VARCHAR(128) NOT NULL, hash_requisicao CHAR(64) NOT NULL,
 campo VARCHAR(10) NOT NULL, contexto_json LONGTEXT NOT NULL, versao_sessao BIGINT NOT NULL,
 estado VARCHAR(30) NOT NULL, resposta_json LONGTEXT NULL, criada_em DATETIME(6) NOT NULL,
 CONSTRAINT fk_leitura_tentativa_sessao FOREIGN KEY (sessao_id) REFERENCES leitura_sessao(id),
 CONSTRAINT fk_leitura_tentativa_imagem FOREIGN KEY (imagem_id) REFERENCES leitura_imagem(id),
 CONSTRAINT uq_leitura_tentativa_idempotencia UNIQUE (sessao_id, chave_idempotencia),
 INDEX ix_leitura_tentativa_fila (estado, campo, criada_em)
) ENGINE=InnoDB;
CREATE TABLE leitura_confirmacao (
 id CHAR(36) PRIMARY KEY, sessao_id CHAR(36) NOT NULL, tentativa_id CHAR(36) NULL,
 campo VARCHAR(10) NOT NULL, origem VARCHAR(20) NOT NULL, item_final_id INT NULL,
 valor_final VARCHAR(255) NULL, motivo VARCHAR(1024) NULL, operador_id INT NOT NULL,
 criada_em DATETIME(6) NOT NULL, versao_sessao BIGINT NOT NULL,
 CONSTRAINT fk_leitura_confirmacao_sessao FOREIGN KEY (sessao_id) REFERENCES leitura_sessao(id),
 CONSTRAINT fk_leitura_confirmacao_tentativa FOREIGN KEY (tentativa_id) REFERENCES leitura_tentativa(id),
 CONSTRAINT fk_leitura_confirmacao_operador FOREIGN KEY (operador_id) REFERENCES usuario(id),
 INDEX ix_leitura_confirmacao_campo (sessao_id, campo, versao_sessao)
) ENGINE=InnoDB;
ALTER TABLE leitura_sessao ADD COLUMN chave_cadastro VARCHAR(128) NULL, ADD COLUMN hash_cadastro CHAR(64) NULL;
