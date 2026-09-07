-- Abandono explícito de sessão: encerra a coleta sem apagar evidência já produzida.
-- Autor e horário são gravados pelo servidor; o motivo acompanha a sessão para a revisão posterior.
ALTER TABLE leitura_sessao ADD COLUMN motivo_abandono VARCHAR(1024) NULL,
 ADD COLUMN abandonada_em DATETIME(6) NULL,
 ADD COLUMN abandonada_por INT NULL,
 ADD CONSTRAINT fk_leitura_sessao_abandono FOREIGN KEY (abandonada_por) REFERENCES usuario(id);
