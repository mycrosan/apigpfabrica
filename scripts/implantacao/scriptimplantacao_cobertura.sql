CREATE TABLE `cobertura` (
  `id` int NOT NULL AUTO_INCREMENT,
  `fotos` json DEFAULT NULL,
  `producao_id` int NOT NULL,
  `dt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` datetime DEFAULT NULL,
  `dt_delete` datetime DEFAULT NULL,
  `usuario_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_cobertura_producao1_idx` (`producao_id`),
  KEY `fk_cobertura_usuario1_idx` (`usuario_id`),
  CONSTRAINT `fk_cobertura_producao1` FOREIGN KEY (`producao_id`) REFERENCES `producao` (`id`),
  CONSTRAINT `fk_cobertura_usuario1` FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=78 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci