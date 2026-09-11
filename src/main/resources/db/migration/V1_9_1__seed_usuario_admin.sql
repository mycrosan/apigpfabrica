-- Seed: Usuario administrador inicial para ambientes novos.
-- Nao duplica se usuario/perfil se ja existirem (idempotente).
-- Login: admin
-- Senha: admin123  (BCrypt rounds=10)
-- Altere a senha apos o primeiro login!

INSERT INTO usuario (login, nome, senha)
SELECT 'admin', 'Administrador Sistema', '$2b$10$yBT0jHARLB.KyPyt1sYBOu40yPPK1qddPswPKmHeVBTaUPlBcgIJ6'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE login = 'admin');

INSERT INTO usuario_perfil (usuario_id, perfil_id)
SELECT u.id, p.id
FROM usuario u
CROSS JOIN perfil p
WHERE u.login = 'admin'
  AND p.descricao = 'ADM'
  AND NOT EXISTS (
    SELECT 1 FROM usuario_perfil up
    WHERE up.usuario_id = u.id AND up.perfil_id = p.id
);
