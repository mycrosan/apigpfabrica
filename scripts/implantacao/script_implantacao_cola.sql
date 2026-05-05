CREATE TABLE `cola` (
  `id` int NOT NULL AUTO_INCREMENT,
  `producao_id` int NOT NULL,
  `data_inicio` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `status` enum('Aguardando','Pronto','Vencido') DEFAULT 'Aguardando',
  `dt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` datetime DEFAULT NULL,
  `usuario_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_cola_producao` (`producao_id`),
  KEY `fk_cola_usuario1_idx` (`usuario_id`),
  CONSTRAINT `fk_cola_producao` FOREIGN KEY (`producao_id`) REFERENCES `producao` (`id`),
  CONSTRAINT `fk_cola_usuario1` FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci