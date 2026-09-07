-- Os fluxos existentes referenciam estes IDs; preservar descrições já cadastradas.
INSERT INTO status_carcaca (id, descricao) SELECT 1, 'Inicial' WHERE NOT EXISTS (SELECT 1 FROM status_carcaca WHERE id = 1);
INSERT INTO status_carcaca (id, descricao) SELECT 2, 'Em produção' WHERE NOT EXISTS (SELECT 1 FROM status_carcaca WHERE id = 2);
INSERT INTO status_carcaca (id, descricao) SELECT 3, 'Aprovada' WHERE NOT EXISTS (SELECT 1 FROM status_carcaca WHERE id = 3);
INSERT INTO status_carcaca (id, descricao) SELECT 4, 'Rejeitada' WHERE NOT EXISTS (SELECT 1 FROM status_carcaca WHERE id = 4);
