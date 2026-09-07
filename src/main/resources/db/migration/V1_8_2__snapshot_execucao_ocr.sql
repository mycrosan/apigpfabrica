CREATE TABLE leitura_execucao (
 tentativa_id CHAR(36) PRIMARY KEY,
 snapshot_json LONGTEXT NOT NULL,
 hash_catalogo CHAR(64) NOT NULL,
 criada_em DATETIME(6) NOT NULL,
 CONSTRAINT fk_leitura_execucao_tentativa FOREIGN KEY (tentativa_id) REFERENCES leitura_tentativa(id)
) ENGINE=InnoDB;
