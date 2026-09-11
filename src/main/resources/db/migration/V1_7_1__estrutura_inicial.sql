-- Estrutura para instalacoes novas e bancos legados existentes (idempotente).
-- Todas as CREATE TABLE usam IF NOT EXISTS.
-- Constraints UNIQUE/FK usam stored procedure temporaria para checar information_schema antes.

DELIMITER $$
DROP PROCEDURE IF EXISTS exec_ddl_if_not_exists $$
CREATE PROCEDURE exec_ddl_if_not_exists(
    IN check_kind ENUM('UNIQUE','FK','INDEX'),
    IN table_name VARCHAR(128),
    IN constraint_name VARCHAR(128),
    IN ddl_stmt TEXT
)
BEGIN
    DECLARE existe INT DEFAULT 0;
    IF check_kind = 'UNIQUE' THEN
        SELECT COUNT(*) INTO existe
        FROM information_schema.table_constraints tc
        WHERE tc.constraint_type = 'UNIQUE'
          AND tc.table_schema = DATABASE()
          AND tc.table_name = table_name
          AND tc.constraint_name = constraint_name;
    ELSEIF check_kind = 'FK' THEN
        SELECT COUNT(*) INTO existe
        FROM information_schema.table_constraints tc
        WHERE tc.constraint_type = 'FOREIGN KEY'
          AND tc.table_schema = DATABASE()
          AND tc.table_name = table_name
          AND tc.constraint_name = constraint_name;
    ELSEIF check_kind = 'INDEX' THEN
        SELECT COUNT(*) INTO existe
        FROM information_schema.statistics s
        WHERE s.index_schema = DATABASE()
          AND s.table_name = table_name
          AND s.index_name = constraint_name;
    END IF;
    IF existe = 0 THEN
        SET @__ddl = ddl_stmt;
        PREPARE stmt FROM @__ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$
DELIMITER ;

CREATE TABLE IF NOT EXISTS antiquebra (id integer not null auto_increment, descricao varchar(255), primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS auditoria (id integer not null auto_increment, registro_id integer not null, usuario_id integer not null, data_acao datetime(6) not null, acao varchar(255) not null, tabela_afetada varchar(255) not null, primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS camelback (id integer not null auto_increment, descricao varchar(255), primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS carcaca (id integer not null auto_increment, medida_id integer, modelo_id integer, pais_id integer, status_carcaca_id integer, usuario_id integer, dt_create datetime(6), dt_update datetime(6), uuid binary(16), dados varchar(255), dot varchar(255), fotos varchar(255), leitura_metadados varchar(255), numero_etiqueta varchar(255), status varchar(255), primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS carcaca_rejeitada (id integer not null auto_increment, medida_id integer, modelo_id integer, pais_id integer, dt_create datetime(6), dt_update datetime(6), uuid binary(16), descricao varchar(255), motivo varchar(255), primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS cobertura (cola_id integer not null, id integer not null auto_increment, usuario_id integer not null, dt_create datetime(6), dt_update datetime(6), fotos json, primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS cola (id integer not null auto_increment, producao_id integer not null, usuario_id integer, data_inicio datetime(6) not null, status enum ('Aguardando','Pronto','Vencido') not null, primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS controle_qualidade (id integer not null auto_increment, producao_id integer, tipo_observacao_id integer, usuario_id integer, dt_create datetime(6), fotos varchar(255), observacao varchar(255), primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS espessuramento (id integer not null auto_increment, descricao varchar(255), primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS maquina_configuracao (id integer not null auto_increment, maquina_id integer not null, matriz_id integer not null, usuario_id integer not null, dt_create datetime(6) not null, dt_delete datetime(6), dt_update datetime(6), celular_id varchar(100) not null, descricao varchar(150), atributos JSON, primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS maquina_registro (id integer not null auto_increment, dt_create datetime(6) not null, dt_delete datetime(6), dt_update datetime(6), nome varchar(100) not null, numero_serie varchar(100), descricao varchar(250), status enum ('Ativa','Inativa','Manutencao') not null, primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS marca (id integer not null auto_increment, descricao varchar(255), primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS matriz (id integer not null auto_increment, descricao varchar(255), primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS medida (id integer not null auto_increment, descricao varchar(255), primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS modelo (id integer not null auto_increment, marca_id integer, descricao varchar(255), primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS motorista (ativo bit not null, id integer not null auto_increment, usuario_id integer not null, data_atualizacao datetime(6), data_criacao datetime(6), cpf varchar(11) not null, observacoes varchar(1024), nome varchar(255) not null, placa_veiculo varchar(255), telefone varchar(255), primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS pais (id integer not null auto_increment, descricao varchar(255), primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS perfil (id integer not null auto_increment, descricao varchar(255), primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS pneus_vulcanizados (id integer not null auto_increment, producao_id integer not null, usuario_id integer not null, dt_create datetime(6) not null, dt_delete datetime(6), dt_update datetime(6), status enum ('FINALIZADO','INICIADO') not null, primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS producao (carcaca_id integer, id integer not null auto_increment, medida_pneu_raspado float(53), regra_id integer, usuario_id integer, dt_create datetime(6), dt_update datetime(6), uuid binary(16), dados varchar(255), fotos varchar(255), primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS regra (antiquebra1_id integer, antiquebra2_id integer, antiquebra3_id integer, camelback_id integer, espessuramento_id integer, id integer not null auto_increment, matriz_id integer, medida_id integer, modelo_id integer, pais_id integer, tamanho_max float(53), tamanho_min float(53), dt_create datetime(6), dt_delete datetime(6), dt_update datetime(6), tempo varchar(255), status enum ('EM_VALIDACAO','VALIDADA'), primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS rele (id integer not null auto_increment, maquina_registro_id integer not null, dt_create datetime(6) not null, dt_delete datetime(6), dt_update datetime(6), celular_id varchar(100) not null, ip varchar(255) not null, primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS status_carcaca (id integer not null auto_increment, descricao varchar(255), primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS tipo_classificacao (id integer not null auto_increment, descricao varchar(255), primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS tipo_observacao (id integer not null auto_increment, tipo_classificacao_id integer, descricao varchar(255), primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS tipo_valida_regra (id integer not null auto_increment, descricao varchar(255), primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS usuario (id integer not null auto_increment, login varchar(255) not null, nome varchar(255) not null, senha varchar(255) not null, primary key (id)) engine=InnoDB;
CREATE TABLE IF NOT EXISTS usuario_perfil (perfil_id integer not null, usuario_id integer not null) engine=InnoDB;
CREATE TABLE IF NOT EXISTS valida_regra (id integer not null auto_increment, qualidade_id integer, regra_id integer, status bit, tipo_valida_regra_id integer, dados varchar(255), primary key (id)) engine=InnoDB;

-- Unique constraints (idempotentes via procedure)
CALL exec_ddl_if_not_exists('UNIQUE', 'carcaca',         'UKjthvfnlpnwi2177vbcwjlrjja', 'ALTER TABLE carcaca ADD CONSTRAINT UKjthvfnlpnwi2177vbcwjlrjja UNIQUE (numero_etiqueta)');
CALL exec_ddl_if_not_exists('UNIQUE', 'maquina_registro', 'UKs4ma428lpp21oy4y8c11p43pt', 'ALTER TABLE maquina_registro ADD CONSTRAINT UKs4ma428lpp21oy4y8c11p43pt UNIQUE (numero_serie)');
CALL exec_ddl_if_not_exists('UNIQUE', 'motorista',        'UKm3qo604s7ko66puhowap8mohy', 'ALTER TABLE motorista ADD CONSTRAINT UKm3qo604s7ko66puhowap8mohy UNIQUE (usuario_id)');
CALL exec_ddl_if_not_exists('UNIQUE', 'motorista',        'UKrbjk7fv6x6kadmtchy9pb5bt3', 'ALTER TABLE motorista ADD CONSTRAINT UKrbjk7fv6x6kadmtchy9pb5bt3 UNIQUE (cpf)');
CALL exec_ddl_if_not_exists('UNIQUE', 'rele',             'UK8juy58eud5mns037hl6f9rr7m', 'ALTER TABLE rele ADD CONSTRAINT UK8juy58eud5mns037hl6f9rr7m UNIQUE (ip)');
CALL exec_ddl_if_not_exists('UNIQUE', 'usuario',          'UKpm3f4m4fqv89oeeeac4tbe2f4', 'ALTER TABLE usuario ADD CONSTRAINT UKpm3f4m4fqv89oeeeac4tbe2f4 UNIQUE (login)');

-- Foreign keys (idempotentes via procedure)
CALL exec_ddl_if_not_exists('FK', 'auditoria',           'FK4ckabg4x8ns3xh5i1l6eam139', 'ALTER TABLE auditoria ADD CONSTRAINT FK4ckabg4x8ns3xh5i1l6eam139 FOREIGN KEY (usuario_id) REFERENCES usuario (id)');
CALL exec_ddl_if_not_exists('FK', 'carcaca',             'FKoxhgdwhce0onq6pdchu44yu3r', 'ALTER TABLE carcaca ADD CONSTRAINT FKoxhgdwhce0onq6pdchu44yu3r FOREIGN KEY (usuario_id) REFERENCES usuario (id)');
CALL exec_ddl_if_not_exists('FK', 'carcaca',             'FKaxc1y4ihqn5h49rr233tiv7tt', 'ALTER TABLE carcaca ADD CONSTRAINT FKaxc1y4ihqn5h49rr233tiv7tt FOREIGN KEY (medida_id) REFERENCES medida (id)');
CALL exec_ddl_if_not_exists('FK', 'carcaca',             'FKma7mijkw7a3bpo3n4bg60u8mv', 'ALTER TABLE carcaca ADD CONSTRAINT FKma7mijkw7a3bpo3n4bg60u8mv FOREIGN KEY (modelo_id) REFERENCES modelo (id)');
CALL exec_ddl_if_not_exists('FK', 'carcaca',             'FK2244m3igin7ng72k1m70tyn66', 'ALTER TABLE carcaca ADD CONSTRAINT FK2244m3igin7ng72k1m70tyn66 FOREIGN KEY (pais_id) REFERENCES pais (id)');
CALL exec_ddl_if_not_exists('FK', 'carcaca',             'FKdxrhbl47hl3ji6pflwvg3i6x3', 'ALTER TABLE carcaca ADD CONSTRAINT FKdxrhbl47hl3ji6pflwvg3i6x3 FOREIGN KEY (status_carcaca_id) REFERENCES status_carcaca (id)');
CALL exec_ddl_if_not_exists('FK', 'carcaca_rejeitada',   'FK73mmfg2exmrx82puh5hoclge1', 'ALTER TABLE carcaca_rejeitada ADD CONSTRAINT FK73mmfg2exmrx82puh5hoclge1 FOREIGN KEY (medida_id) REFERENCES medida (id)');
CALL exec_ddl_if_not_exists('FK', 'carcaca_rejeitada',   'FKacs4lwmix7374y367i0ihr1t4', 'ALTER TABLE carcaca_rejeitada ADD CONSTRAINT FKacs4lwmix7374y367i0ihr1t4 FOREIGN KEY (modelo_id) REFERENCES modelo (id)');
CALL exec_ddl_if_not_exists('FK', 'carcaca_rejeitada',   'FKay7lcmialgn755hc5674rc7j5', 'ALTER TABLE carcaca_rejeitada ADD CONSTRAINT FKay7lcmialgn755hc5674rc7j5 FOREIGN KEY (pais_id) REFERENCES pais (id)');
CALL exec_ddl_if_not_exists('FK', 'cobertura',           'FKc5rmc9n1wmf3rr3c8hdd2dmhc', 'ALTER TABLE cobertura ADD CONSTRAINT FKc5rmc9n1wmf3rr3c8hdd2dmhc FOREIGN KEY (cola_id) REFERENCES cola (id)');
CALL exec_ddl_if_not_exists('FK', 'cobertura',           'FKgo6vaas2dt27ma4vigbx6nrke', 'ALTER TABLE cobertura ADD CONSTRAINT FKgo6vaas2dt27ma4vigbx6nrke FOREIGN KEY (usuario_id) REFERENCES usuario (id)');
CALL exec_ddl_if_not_exists('FK', 'cola',                'FKf30p3ag0jbh4oop9x96hj4y68', 'ALTER TABLE cola ADD CONSTRAINT FKf30p3ag0jbh4oop9x96hj4y68 FOREIGN KEY (producao_id) REFERENCES producao (id)');
CALL exec_ddl_if_not_exists('FK', 'cola',                'FKtnb974hsaa2vm65guew6dpffk', 'ALTER TABLE cola ADD CONSTRAINT FKtnb974hsaa2vm65guew6dpffk FOREIGN KEY (usuario_id) REFERENCES usuario (id)');
CALL exec_ddl_if_not_exists('FK', 'controle_qualidade',  'FK32sbjc359i4t65cc2psesbk91', 'ALTER TABLE controle_qualidade ADD CONSTRAINT FK32sbjc359i4t65cc2psesbk91 FOREIGN KEY (producao_id) REFERENCES producao (id)');
CALL exec_ddl_if_not_exists('FK', 'controle_qualidade',  'FK1divmcbnktv0vjw4sxob7u1f6', 'ALTER TABLE controle_qualidade ADD CONSTRAINT FK1divmcbnktv0vjw4sxob7u1f6 FOREIGN KEY (tipo_observacao_id) REFERENCES tipo_observacao (id)');
CALL exec_ddl_if_not_exists('FK', 'controle_qualidade',  'FK44im9wp9xgrio521fd0ucl09g', 'ALTER TABLE controle_qualidade ADD CONSTRAINT FK44im9wp9xgrio521fd0ucl09g FOREIGN KEY (usuario_id) REFERENCES usuario (id)');
CALL exec_ddl_if_not_exists('FK', 'maquina_configuracao','FKqg2omb2o1e1spgu1xixlmkhhs', 'ALTER TABLE maquina_configuracao ADD CONSTRAINT FKqg2omb2o1e1spgu1xixlmkhhs FOREIGN KEY (maquina_id) REFERENCES maquina_registro (id)');
CALL exec_ddl_if_not_exists('FK', 'maquina_configuracao','FK4hgj8hx2bdr45706vwt1v4vve', 'ALTER TABLE maquina_configuracao ADD CONSTRAINT FK4hgj8hx2bdr45706vwt1v4vve FOREIGN KEY (matriz_id) REFERENCES matriz (id)');
CALL exec_ddl_if_not_exists('FK', 'modelo',              'FKllxq2dldvhxvb5q9csar7vdfy', 'ALTER TABLE modelo ADD CONSTRAINT FKllxq2dldvhxvb5q9csar7vdfy FOREIGN KEY (marca_id) REFERENCES marca (id)');
CALL exec_ddl_if_not_exists('FK', 'motorista',           'FKjpl954mh4vubwwed4m0gl55gn', 'ALTER TABLE motorista ADD CONSTRAINT FKjpl954mh4vubwwed4m0gl55gn FOREIGN KEY (usuario_id) REFERENCES usuario (id)');
CALL exec_ddl_if_not_exists('FK', 'pneus_vulcanizados',  'FKkic2yrfsf4tv2y7iypuw7xfsp', 'ALTER TABLE pneus_vulcanizados ADD CONSTRAINT FKkic2yrfsf4tv2y7iypuw7xfsp FOREIGN KEY (producao_id) REFERENCES producao (id)');
CALL exec_ddl_if_not_exists('FK', 'pneus_vulcanizados',  'FKowpkr9vx27ub4steefx8af9v2', 'ALTER TABLE pneus_vulcanizados ADD CONSTRAINT FKowpkr9vx27ub4steefx8af9v2 FOREIGN KEY (usuario_id) REFERENCES usuario (id)');
CALL exec_ddl_if_not_exists('FK', 'producao',            'FKgx9p0o6ilirftv5pjhm8uvqe3', 'ALTER TABLE producao ADD CONSTRAINT FKgx9p0o6ilirftv5pjhm8uvqe3 FOREIGN KEY (carcaca_id) REFERENCES carcaca (id)');
CALL exec_ddl_if_not_exists('FK', 'producao',            'FKfcuetqkerwhg2lp6na001tw7i', 'ALTER TABLE producao ADD CONSTRAINT FKfcuetqkerwhg2lp6na001tw7i FOREIGN KEY (usuario_id) REFERENCES usuario (id)');
CALL exec_ddl_if_not_exists('FK', 'producao',            'FK2u3uwf3cauga0agx2sbnwodut', 'ALTER TABLE producao ADD CONSTRAINT FK2u3uwf3cauga0agx2sbnwodut FOREIGN KEY (regra_id) REFERENCES regra (id)');
CALL exec_ddl_if_not_exists('FK', 'regra',               'FK66laomgvctepp0pxota0ocskg', 'ALTER TABLE regra ADD CONSTRAINT FK66laomgvctepp0pxota0ocskg FOREIGN KEY (antiquebra1_id) REFERENCES antiquebra (id)');
CALL exec_ddl_if_not_exists('FK', 'regra',               'FKai5f7vib668t8h3dy17aefd5t', 'ALTER TABLE regra ADD CONSTRAINT FKai5f7vib668t8h3dy17aefd5t FOREIGN KEY (antiquebra2_id) REFERENCES antiquebra (id)');
CALL exec_ddl_if_not_exists('FK', 'regra',               'FKdnk7pungklvrlvc0lwl1e59c1', 'ALTER TABLE regra ADD CONSTRAINT FKdnk7pungklvrlvc0lwl1e59c1 FOREIGN KEY (antiquebra3_id) REFERENCES antiquebra (id)');
CALL exec_ddl_if_not_exists('FK', 'regra',               'FKgmanphdkr2iv3b3e3et9u582l', 'ALTER TABLE regra ADD CONSTRAINT FKgmanphdkr2iv3b3e3et9u582l FOREIGN KEY (camelback_id) REFERENCES camelback (id)');
CALL exec_ddl_if_not_exists('FK', 'regra',               'FK6sd2trwb81xgqyiwnap8ebp8g', 'ALTER TABLE regra ADD CONSTRAINT FK6sd2trwb81xgqyiwnap8ebp8g FOREIGN KEY (espessuramento_id) REFERENCES espessuramento (id)');
CALL exec_ddl_if_not_exists('FK', 'regra',               'FK8pwueox2et4qa0aexuhbrhcv7', 'ALTER TABLE regra ADD CONSTRAINT FK8pwueox2et4qa0aexuhbrhcv7 FOREIGN KEY (matriz_id) REFERENCES matriz (id)');
CALL exec_ddl_if_not_exists('FK', 'regra',               'FK1p2poyrv7qf7sy2ne8y9v9ogc', 'ALTER TABLE regra ADD CONSTRAINT FK1p2poyrv7qf7sy2ne8y9v9ogc FOREIGN KEY (medida_id) REFERENCES medida (id)');
CALL exec_ddl_if_not_exists('FK', 'regra',               'FKh5yhjb834k7yv91nv1gkfhgpu', 'ALTER TABLE regra ADD CONSTRAINT FKh5yhjb834k7yv91nv1gkfhgpu FOREIGN KEY (modelo_id) REFERENCES modelo (id)');
CALL exec_ddl_if_not_exists('FK', 'regra',               'FKpgl4xhssb74jmsq6wyxu3r7w7', 'ALTER TABLE regra ADD CONSTRAINT FKpgl4xhssb74jmsq6wyxu3r7w7 FOREIGN KEY (pais_id) REFERENCES pais (id)');
CALL exec_ddl_if_not_exists('FK', 'rele',                'FKj9xaqqh8egsgvd7rnd2dthyac', 'ALTER TABLE rele ADD CONSTRAINT FKj9xaqqh8egsgvd7rnd2dthyac FOREIGN KEY (maquina_registro_id) REFERENCES maquina_registro (id)');
CALL exec_ddl_if_not_exists('FK', 'tipo_observacao',     'FKhfg9dho0d12phh5h59bcerk2h', 'ALTER TABLE tipo_observacao ADD CONSTRAINT FKhfg9dho0d12phh5h59bcerk2h FOREIGN KEY (tipo_classificacao_id) REFERENCES tipo_classificacao (id)');
CALL exec_ddl_if_not_exists('FK', 'usuario_perfil',      'FK22cgfn0obntlvqyfn33pyk24d', 'ALTER TABLE usuario_perfil ADD CONSTRAINT FK22cgfn0obntlvqyfn33pyk24d FOREIGN KEY (perfil_id) REFERENCES perfil (id)');
CALL exec_ddl_if_not_exists('FK', 'usuario_perfil',      'FKnrjqnbylalt4ykxbcef24f57w', 'ALTER TABLE usuario_perfil ADD CONSTRAINT FKnrjqnbylalt4ykxbcef24f57w FOREIGN KEY (usuario_id) REFERENCES usuario (id)');
CALL exec_ddl_if_not_exists('FK', 'valida_regra',        'FK4hflho3eiq4tpsfudl6fku0fy', 'ALTER TABLE valida_regra ADD CONSTRAINT FK4hflho3eiq4tpsfudl6fku0fy FOREIGN KEY (qualidade_id) REFERENCES controle_qualidade (id)');
CALL exec_ddl_if_not_exists('FK', 'valida_regra',        'FKgvqilhcd5705umlsk29bu2rl6', 'ALTER TABLE valida_regra ADD CONSTRAINT FKgvqilhcd5705umlsk29bu2rl6 FOREIGN KEY (regra_id) REFERENCES regra (id)');
CALL exec_ddl_if_not_exists('FK', 'valida_regra',        'FKlrl8o4x3hksl47utc8ftdcah6', 'ALTER TABLE valida_regra ADD CONSTRAINT FKlrl8o4x3hksl47utc8ftdcah6 FOREIGN KEY (tipo_valida_regra_id) REFERENCES tipo_valida_regra (id)');

DROP PROCEDURE IF EXISTS exec_ddl_if_not_exists;
