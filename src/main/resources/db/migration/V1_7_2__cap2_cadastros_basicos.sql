-- CAPÍTULO 2 — CADASTROS BÁSICOS (Compatível com MySQL 8)
-- Restrições atendidas:
-- - NÃO alterar tabelas existentes
-- - NÃO usar DELETE para dados operacionais (soft delete via campo 'ativo')
-- - Usar InnoDB e nomes claros
-- - Manter compatibilidade com banco atual

-- 1) PERFIS (papéis do sistema)
-- Observação: Tabela 'perfil' já existe no banco atual (id, descricao).
-- Inserções iniciais, sem duplicar valores existentes.

INSERT INTO perfil (descricao)
SELECT 'ADM' WHERE NOT EXISTS (SELECT 1 FROM perfil WHERE descricao = 'ADM');

INSERT INTO perfil (descricao)
SELECT 'MOTORISTA' WHERE NOT EXISTS (SELECT 1 FROM perfil WHERE descricao = 'MOTORISTA');

INSERT INTO perfil (descricao)
SELECT 'INSPETOR' WHERE NOT EXISTS (SELECT 1 FROM perfil WHERE descricao = 'INSPETOR');

INSERT INTO perfil (descricao)
SELECT 'RASPADOR' WHERE NOT EXISTS (SELECT 1 FROM perfil WHERE descricao = 'RASPADOR');

INSERT INTO perfil (descricao)
SELECT 'VENDEDOR' WHERE NOT EXISTS (SELECT 1 FROM perfil WHERE descricao = 'VENDEDOR');

INSERT INTO perfil (descricao)
SELECT 'CONSULTA' WHERE NOT EXISTS (SELECT 1 FROM perfil WHERE descricao = 'CONSULTA');

-- 2) USUARIOS_PERFIS (N:N)
-- Observação: Tabela de relacionamento já existe com o nome 'usuario_perfil'.
-- Estrutura atual utiliza chave primária surrogate 'id'. Para compatibilidade, não será alterada.
-- Recomendação (comentada, opcional):
-- ALTER TABLE usuario_perfil ADD UNIQUE KEY uq_usuario_perfil (usuario_id, perfil_id);

-- 3) MOTORISTAS (entidade própria, vinculada 1:1 ao usuário)
-- Regras:
-- - usuario_id deve ser UNIQUE
-- - Soft delete via campo 'ativo'
-- - Não permitir exclusão física (aplicação deve usar UPDATE ativo=false)

CREATE TABLE IF NOT EXISTS motorista (
  id INT NOT NULL AUTO_INCREMENT,
  nome VARCHAR(255) NOT NULL,
  cpf CHAR(11) NOT NULL,
  telefone VARCHAR(20) NULL,
  placa_veiculo VARCHAR(10) NULL,
  observacoes VARCHAR(1024) NULL,
  usuario_id INT NOT NULL,
  ativo TINYINT(1) NOT NULL DEFAULT 1,
  data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  data_atualizacao DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_motorista_cpf (cpf),
  UNIQUE KEY uq_motorista_usuario (usuario_id),
  INDEX idx_motorista_ativo (ativo),
  CONSTRAINT fk_motorista_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4) Regras de negócio (pseudo-código / comentários)
-- a) Usuário com perfil MOTORISTA não pode ter outro perfil
--    Exemplo (nível aplicação):
--    if (perfil == 'MOTORISTA' && usuario.possuiOutrosPerfis()) throw ErroRegraNegocio;
--
-- b) Motorista não pode ser excluído se já participou de compras
--    Exemplo (nível aplicação):
--    if (existeRegistroEm('compra', motorista_id)) throw ErroRegraNegocio;
--    else UPDATE motorista SET ativo = 0 WHERE id = :id;
--
-- c) Listas operacionais devem mostrar apenas registros ativos
--    SELECT * FROM motorista WHERE ativo = 1;

-- 5) AUDITORIA (genérica para uso futuro)
-- Campos: tabela_afetada, registro_id, ação (INSERT, UPDATE, INATIVAR), usuário responsável, data da ação

CREATE TABLE IF NOT EXISTS auditoria (
  id INT NOT NULL AUTO_INCREMENT,
  tabela_afetada VARCHAR(100) NOT NULL,
  registro_id INT NOT NULL,
  acao ENUM('INSERT','UPDATE','INATIVAR') NOT NULL,
  usuario_id INT NOT NULL,
  data_acao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_auditoria_tabela_registro (tabela_afetada, registro_id),
  INDEX idx_auditoria_usuario (usuario_id),
  CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
