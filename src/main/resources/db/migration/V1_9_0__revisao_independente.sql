CREATE TABLE revisao_imagem (
 id CHAR(36) PRIMARY KEY, sha256 CHAR(64) NOT NULL UNIQUE,
 arquivo VARCHAR(255) NOT NULL, previa VARCHAR(255) NOT NULL, hash_previa CHAR(64) NOT NULL,
 largura INT NOT NULL, altura INT NOT NULL, criada_em DATETIME(6) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE revisao_item (
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
CREATE TABLE revisao_resposta (
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
INSERT INTO perfil (descricao) SELECT 'REVISAR_LEITURA'
 WHERE NOT EXISTS (SELECT 1 FROM perfil WHERE descricao = 'REVISAR_LEITURA');
INSERT INTO perfil (descricao) SELECT 'IMPORTAR_LEITURA'
 WHERE NOT EXISTS (SELECT 1 FROM perfil WHERE descricao = 'IMPORTAR_LEITURA');
