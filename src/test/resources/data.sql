-- Script de inicialização para testes
-- Inserir dados básicos necessários para os testes

-- Inserir perfil padrão
INSERT INTO perfil (id, descricao) VALUES (1, 'ROLE_USER');

-- Inserir usuário de teste
INSERT INTO usuario (id, nome, login, senha) VALUES 
(1, 'Usuario Base', 'base@teste.com', '$2a$10$HvzA2YBbZQyQbfeXfJTlUezu5H7kqxjEuqXEyRXxgYxmOqrEeVBpm');

-- Inserir relação usuário-perfil
INSERT INTO usuario_perfil (perfil_id, usuario_id) VALUES (1, 1);

-- Inserir matriz de teste
INSERT INTO matriz (id, descricao) VALUES (1, 'Matriz Teste');

-- Inserir máquina de teste
INSERT INTO maquina_registro (id, nome, descricao, status, numero_serie, dt_create) VALUES 
(1, 'Máquina Teste', 'Máquina para testes', 'Ativa', 'SN-TEST-001', NOW());

-- Inserir status de carcaça necessários para o fluxo
INSERT INTO status_carcaca (id, descricao) VALUES 
(2, 'Em produção'),
(3, 'Qualificado - Aprovado'),
(4, 'Qualificado - Reprovado');

-- Inserir classificações de qualidade
INSERT INTO tipo_classificacao (id, descricao) VALUES 
(1, 'APROVADO'),
(2, 'REPROVADO');

-- Inserir tipo de observação padrão vinculado a APROVADO
INSERT INTO tipo_observacao (id, descricao, tipo_classificacao_id) VALUES 
(1, 'Observação Padrão', 1);