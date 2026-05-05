CREATE TABLE `antiquebra` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8;
CREATE TABLE `camelback` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8;
CREATE TABLE `carcaca` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `numero_etiqueta` varchar(45) NOT NULL,
  `dot` char(10) NOT NULL COMMENT 'DOT= Department of Transportation, informa que o pneu está em conformidade com os regulamentos DOT dos Estados Unidos. O código começa com as letras DOT seguido por duas letras e o número que representa a fábrica onde foi produzido.',
  `dados` json NOT NULL,
  `modelo_id` int(11) NOT NULL,
  `medida_id` int(11) NOT NULL,
  `pais_id` int(11) NOT NULL,
  `fotos` json DEFAULT NULL,
  `status_carcaca_id` int(11) DEFAULT NULL,
  `status` varchar(45) NOT NULL DEFAULT 'start',
  `dt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` datetime DEFAULT NULL,
  `dt_delete` datetime DEFAULT NULL,
  `uuid` binary(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_pneu_modelo1_idx` (`modelo_id`),
  KEY `fk_pneu_medida1_idx` (`medida_id`),
  KEY `fk_pneu_pais1_idx` (`pais_id`),
  KEY `fk_carcaca_status_carcaca1_idx` (`status_carcaca_id`),
  CONSTRAINT `fk_carcaca_status_carcaca1` FOREIGN KEY (`status_carcaca_id`) REFERENCES `status_carcaca` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_pneu_medida1` FOREIGN KEY (`medida_id`) REFERENCES `medida` (`id`),
  CONSTRAINT `fk_pneu_modelo1` FOREIGN KEY (`modelo_id`) REFERENCES `modelo` (`id`),
  CONSTRAINT `fk_pneu_pais1` FOREIGN KEY (`pais_id`) REFERENCES `pais` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2266 DEFAULT CHARSET=utf8;
CREATE TABLE `controle_qualidade` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `producao_id` int(11) NOT NULL,
  `observacao` varchar(45) DEFAULT NULL,
  `tipo_classificacao_id` int(11) NOT NULL COMMENT 'NET\\nLOJA\\nLOJA C\\nD\\nESCARIAR\\nDESCARTE',
  `fotos` json NOT NULL,
  `tipo_observacao_id` int(11) NOT NULL COMMENT 'BOLHA POR DENTRO\\nFALHA NA ANTIQUEBRA\\nTALAO FROUXO\\nBOLHA EXTERNA QDO ESTAVA QUENTE',
  PRIMARY KEY (`id`),
  KEY `fk_controle_qualidade_producao1_idx` (`producao_id`),
  KEY `fk_controle_qualidade_tipo_classificacao1_idx` (`tipo_classificacao_id`),
  KEY `fk_controle_qualidade_tipo_observacao1_idx` (`tipo_observacao_id`),
  CONSTRAINT `fk_controle_qualidade_producao1` FOREIGN KEY (`producao_id`) REFERENCES `producao` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE `correcao` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `peso_antes` json NOT NULL,
  `peso_depois` json NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE `credenciados` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `contrato_social` varchar(45) CHARACTER SET utf8 NOT NULL,
  `fotos` json NOT NULL,
  `cnpj` char(14) CHARACTER SET utf8 NOT NULL,
  `nm_fantasia` varchar(45) CHARACTER SET utf8 NOT NULL,
  `nm_gerente_loja` varchar(45) CHARACTER SET utf8 NOT NULL,
  `celular_gerente_loja` varchar(45) CHARACTER SET utf8 NOT NULL,
  `email_loja` varchar(45) CHARACTER SET utf8 NOT NULL,
  `telefone_loja` varchar(45) CHARACTER SET utf8 DEFAULT NULL,
  `site` varchar(45) CHARACTER SET utf8 DEFAULT NULL,
  `logradouro` varchar(45) CHARACTER SET utf8 DEFAULT NULL,
  `numero` varchar(45) CHARACTER SET utf8 DEFAULT NULL,
  `complemento` varchar(45) CHARACTER SET utf8 DEFAULT NULL,
  `bairro` varchar(45) CHARACTER SET utf8 DEFAULT NULL,
  `cidade` varchar(45) CHARACTER SET utf8 DEFAULT NULL,
  `uf` varchar(45) CHARACTER SET utf8 DEFAULT NULL,
  `cep` char(8) CHARACTER SET utf8 DEFAULT NULL,
  `lat` varchar(45) CHARACTER SET utf8 DEFAULT NULL,
  `log` varchar(45) CHARACTER SET utf8 DEFAULT NULL,
  `status` varchar(10) CHARACTER SET utf8 NOT NULL DEFAULT 'PENDENTE',
  `dados_adicionais` json DEFAULT NULL,
  `dt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` datetime DEFAULT NULL,
  `dt_delete` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `espessuramento` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int(11) NOT NULL,
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int(11) DEFAULT NULL,
  `installed_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int(11) NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `marca` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=108 DEFAULT CHARSET=utf8;
CREATE TABLE `matriz` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=55 DEFAULT CHARSET=utf8;
CREATE TABLE `medida` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8;
CREATE TABLE `modelo` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  `marca_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_modelo_marca1_idx` (`marca_id`),
  CONSTRAINT `fk_modelo_marca1` FOREIGN KEY (`marca_id`) REFERENCES `marca` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=471 DEFAULT CHARSET=utf8;
CREATE TABLE `pais` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8;
CREATE TABLE `perfil` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
CREATE TABLE `producao` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `carcaca_id` int(11) NOT NULL,
  `medida_pneu_raspado` decimal(4,3) NOT NULL,
  `dados` json NOT NULL,
  `regra_id` int(11) NOT NULL,
  `fotos` json DEFAULT NULL,
  `dt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` datetime DEFAULT NULL,
  `dt_delete` datetime DEFAULT NULL,
  `uuid` binary(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_producao_pneu1_idx` (`carcaca_id`),
  KEY `fk_producao_regra1_idx` (`regra_id`),
  CONSTRAINT `fk_producao_pneu1` FOREIGN KEY (`carcaca_id`) REFERENCES `carcaca` (`id`),
  CONSTRAINT `fk_producao_regra1` FOREIGN KEY (`regra_id`) REFERENCES `regra` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1276 DEFAULT CHARSET=utf8;
CREATE TABLE `regra` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tamanho_min` decimal(4,3) NOT NULL,
  `tamanho_max` decimal(4,3) NOT NULL,
  `tempo` varchar(45) DEFAULT NULL,
  `matriz_id` int(11) NOT NULL,
  `medida_id` int(11) NOT NULL,
  `pais_id` int(11) NOT NULL,
  `modelo_id` int(11) NOT NULL,
  `camelback_id` int(11) NOT NULL,
  `espessuramento_id` int(11) DEFAULT NULL,
  `antiquebra1_id` int(11) NOT NULL,
  `antiquebra2_id` int(11) DEFAULT NULL,
  `antiquebra3_id` int(11) DEFAULT NULL,
  `dt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` datetime DEFAULT NULL,
  `dt_delete` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_regra_matriz1_idx` (`matriz_id`),
  KEY `fk_regra_medida1_idx` (`medida_id`),
  KEY `fk_regra_pais1_idx` (`pais_id`),
  KEY `fk_regra_modelo1_idx` (`modelo_id`),
  KEY `fk_regra_camelback1_idx` (`camelback_id`),
  KEY `fk_regra_espessuramento1_idx` (`espessuramento_id`),
  KEY `fk_regra_antiquebra1_idx` (`antiquebra1_id`),
  KEY `fk_regra_antiquebra2_idx` (`antiquebra2_id`),
  KEY `fk_regra_antiquebra3_idx` (`antiquebra3_id`),
  CONSTRAINT `fk_regra_antiquebra1` FOREIGN KEY (`antiquebra1_id`) REFERENCES `antiquebra` (`id`),
  CONSTRAINT `fk_regra_antiquebra2` FOREIGN KEY (`antiquebra2_id`) REFERENCES `antiquebra` (`id`),
  CONSTRAINT `fk_regra_antiquebra3` FOREIGN KEY (`antiquebra3_id`) REFERENCES `antiquebra` (`id`),
  CONSTRAINT `fk_regra_camelback1` FOREIGN KEY (`camelback_id`) REFERENCES `camelback` (`id`),
  CONSTRAINT `fk_regra_espessuramento1` FOREIGN KEY (`espessuramento_id`) REFERENCES `espessuramento` (`id`),
  CONSTRAINT `fk_regra_matriz1` FOREIGN KEY (`matriz_id`) REFERENCES `matriz` (`id`),
  CONSTRAINT `fk_regra_medida1` FOREIGN KEY (`medida_id`) REFERENCES `medida` (`id`),
  CONSTRAINT `fk_regra_modelo1` FOREIGN KEY (`modelo_id`) REFERENCES `modelo` (`id`),
  CONSTRAINT `fk_regra_pais1` FOREIGN KEY (`pais_id`) REFERENCES `pais` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=431 DEFAULT CHARSET=utf8;
CREATE TABLE `tipo_classificacao` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE `tipo_observacao` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE `usuario` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nome` varchar(45) DEFAULT NULL,
  `login` varchar(45) DEFAULT NULL,
  `senha` varchar(250) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
CREATE TABLE `usuario_perfil` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `perfil_id` int(11) NOT NULL,
  `usuario_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_usuario_perfil_perfil1_idx` (`perfil_id`),
  KEY `fk_usuario_perfil_usuario1_idx` (`usuario_id`),
  CONSTRAINT `fk_usuario_perfil_perfil1` FOREIGN KEY (`perfil_id`) REFERENCES `perfil` (`id`),
  CONSTRAINT `fk_usuario_perfil_usuario1` FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
