ALTER TABLE leitura_sessao
 ADD COLUMN carcaca_inicial_id INT NULL,
 ADD COLUMN motivo_combinacao_nova VARCHAR(1024) NULL;
UPDATE leitura_sessao SET carcaca_inicial_id = carcaca_id WHERE status = 'ABERTA';
