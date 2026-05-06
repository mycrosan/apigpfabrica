-- MySQL dump 10.13  Distrib 8.0.43, for macos15 (arm64)
--
-- Host: localhost    Database: gppremium
-- ------------------------------------------------------
-- Server version	9.4.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `antiquebra`
--

DROP TABLE IF EXISTS `antiquebra`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `antiquebra` (
  `id` int NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=48 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `auditoria`
--

DROP TABLE IF EXISTS `auditoria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auditoria` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `acao` varchar(255) NOT NULL,
  `data_acao` datetime(6) NOT NULL,
  `registro_id` bigint NOT NULL,
  `tabela_afetada` varchar(255) NOT NULL,
  `usuario_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `camelback`
--

DROP TABLE IF EXISTS `camelback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `camelback` (
  `id` int NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=70 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `carcaca`
--

DROP TABLE IF EXISTS `carcaca`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carcaca` (
  `id` bigint NOT NULL,
  `numero_etiqueta` varchar(45) NOT NULL,
  `dot` varchar(255) DEFAULT NULL,
  `dados` json NOT NULL,
  `modelo_id` int DEFAULT NULL,
  `medida_id` int NOT NULL,
  `pais_id` int DEFAULT NULL,
  `fotos` json DEFAULT NULL,
  `status_carcaca_id` int DEFAULT NULL,
  `status` varchar(45) NOT NULL DEFAULT 'start',
  `dt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` datetime DEFAULT NULL,
  `dt_delete` datetime DEFAULT NULL,
  `uuid` binary(16) DEFAULT NULL,
  `localizacao_id` int DEFAULT NULL,
  `old_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_carcaca_numero_etiqueta` (`numero_etiqueta`),
  KEY `fk_pneu_modelo1_idx` (`modelo_id`),
  KEY `fk_pneu_medida1_idx` (`medida_id`),
  KEY `fk_pneu_pais1_idx` (`pais_id`),
  KEY `fk_carcaca_status_carcaca1_idx` (`status_carcaca_id`),
  KEY `FKm0m3mlwn0rqdc23q3liy4bwe0` (`localizacao_id`),
  CONSTRAINT `fk_carcaca_status_carcaca1` FOREIGN KEY (`status_carcaca_id`) REFERENCES `status_carcaca` (`id`),
  CONSTRAINT `fk_pneu_medida1` FOREIGN KEY (`medida_id`) REFERENCES `medida` (`id`),
  CONSTRAINT `fk_pneu_modelo1` FOREIGN KEY (`modelo_id`) REFERENCES `modelo` (`id`),
  CONSTRAINT `fk_pneu_pais1` FOREIGN KEY (`pais_id`) REFERENCES `pais` (`id`),
  CONSTRAINT `FKm0m3mlwn0rqdc23q3liy4bwe0` FOREIGN KEY (`localizacao_id`) REFERENCES `localizacao` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `carcaca_movimentacao`
--

DROP TABLE IF EXISTS `carcaca_movimentacao`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carcaca_movimentacao` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dt_evento` datetime(6) NOT NULL,
  `novo_status` varchar(255) NOT NULL,
  `observacoes` varchar(1024) DEFAULT NULL,
  `status_anterior` varchar(255) NOT NULL,
  `carcaca_id` bigint DEFAULT NULL,
  `usuario_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKrl83u3c64tq1tlg43nrvrruqi` (`carcaca_id`),
  KEY `FKj2atydwua7udi6oxxolrqffxf` (`usuario_id`),
  CONSTRAINT `FKj2atydwua7udi6oxxolrqffxf` FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `carcaca_rejeitada`
--

DROP TABLE IF EXISTS `carcaca_rejeitada`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carcaca_rejeitada` (
  `id` int NOT NULL AUTO_INCREMENT,
  `modelo_id` int NOT NULL,
  `medida_id` int NOT NULL,
  `pais_id` int NOT NULL,
  `dt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` datetime DEFAULT NULL,
  `dt_delete` datetime DEFAULT NULL,
  `uuid` binary(16) DEFAULT NULL,
  `motivo` varchar(250) DEFAULT NULL,
  `descricao` varchar(250) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_pneu_modelo1_idx` (`modelo_id`),
  KEY `fk_pneu_medida1_idx` (`medida_id`),
  KEY `fk_pneu_pais1_idx` (`pais_id`),
  CONSTRAINT `fk_pneu_medida10` FOREIGN KEY (`medida_id`) REFERENCES `medida` (`id`),
  CONSTRAINT `fk_pneu_modelo10` FOREIGN KEY (`modelo_id`) REFERENCES `modelo` (`id`),
  CONSTRAINT `fk_pneu_pais10` FOREIGN KEY (`pais_id`) REFERENCES `pais` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2622 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `carcaca_status_historico`
--

DROP TABLE IF EXISTS `carcaca_status_historico`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carcaca_status_historico` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `carcaca_id` bigint NOT NULL,
  `status_origem_id` int DEFAULT NULL,
  `status_destino_id` int NOT NULL,
  `usuario_id` int NOT NULL,
  `data_hora` datetime NOT NULL,
  `motivo` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_carcaca_status_hist_carcaca` (`carcaca_id`),
  KEY `fk_carcaca_status_hist_status_origem` (`status_origem_id`),
  KEY `fk_carcaca_status_hist_status_dest` (`status_destino_id`),
  KEY `fk_carcaca_status_hist_usuario` (`usuario_id`),
  CONSTRAINT `fk_carcaca_status_hist_carcaca` FOREIGN KEY (`carcaca_id`) REFERENCES `carcaca` (`id`),
  CONSTRAINT `fk_carcaca_status_hist_status_dest` FOREIGN KEY (`status_destino_id`) REFERENCES `status_carcaca` (`id`),
  CONSTRAINT `fk_carcaca_status_hist_status_origem` FOREIGN KEY (`status_origem_id`) REFERENCES `status_carcaca` (`id`),
  CONSTRAINT `fk_carcaca_status_hist_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cliente`
--

DROP TABLE IF EXISTS `cliente`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cliente` (
  `id` int NOT NULL AUTO_INCREMENT,
  `ativo` bit(1) NOT NULL,
  `cidade` varchar(80) DEFAULT NULL,
  `cnpj_cpf` varchar(18) DEFAULT NULL,
  `contato` varchar(80) DEFAULT NULL,
  `data_atualizacao` datetime(6) DEFAULT NULL,
  `data_criacao` datetime(6) NOT NULL,
  `estado` varchar(2) DEFAULT NULL,
  `loja_grupo` bit(1) NOT NULL,
  `nome` varchar(150) NOT NULL,
  `nome_fantasia` varchar(80) DEFAULT NULL,
  `observacoes` text,
  `telefone` varchar(30) DEFAULT NULL,
  `usuario_responsavel_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_stw573p7lmwup5wjsvff7vs6i` (`cnpj_cpf`),
  KEY `idx_cliente_ativo` (`ativo`),
  KEY `idx_cliente_nome` (`nome`),
  KEY `idx_cliente_loja_grupo` (`loja_grupo`),
  KEY `idx_cliente_cidade_estado` (`cidade`,`estado`),
  KEY `FKok6rgpn8hbxwecpr1n0dwpous` (`usuario_responsavel_id`),
  CONSTRAINT `FKok6rgpn8hbxwecpr1n0dwpous` FOREIGN KEY (`usuario_responsavel_id`) REFERENCES `usuario` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cobertura`
--

DROP TABLE IF EXISTS `cobertura`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cobertura` (
  `id` int NOT NULL AUTO_INCREMENT,
  `fotos` json DEFAULT NULL,
  `producao_id` int DEFAULT NULL,
  `dt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` datetime DEFAULT NULL,
  `dt_delete` datetime DEFAULT NULL,
  `usuario_id` int NOT NULL,
  `cola_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_cobertura_producao1_idx` (`producao_id`),
  KEY `fk_cobertura_usuario1_idx` (`usuario_id`),
  KEY `fk_cobertura_cola1_idx` (`cola_id`),
  CONSTRAINT `fk_cobertura_cola1` FOREIGN KEY (`cola_id`) REFERENCES `cola` (`id`),
  CONSTRAINT `fk_cobertura_producao1` FOREIGN KEY (`producao_id`) REFERENCES `producao` (`id`),
  CONSTRAINT `fk_cobertura_usuario1` FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7048 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cola`
--

DROP TABLE IF EXISTS `cola`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=7121 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `compra`
--

DROP TABLE IF EXISTS `compra`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `compra` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `valor_total` decimal(19,2) DEFAULT NULL,
  `fornecedor_id` int DEFAULT NULL,
  `usuario_id` bigint DEFAULT NULL,
  `observacoes` varchar(1024) DEFAULT NULL,
  `total_itens` int DEFAULT NULL,
  `qtd_negociada` int DEFAULT NULL,
  `qtd_coletada` int DEFAULT NULL,
  `saldo_pendente` int DEFAULT NULL,
  `status_transicao_id` int DEFAULT NULL,
  `dt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` datetime DEFAULT NULL,
  `dt_delete` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK4go8vtn4msugmpbf6wwkpumkb` (`fornecedor_id`),
  KEY `fk_compra_status_transicao` (`status_transicao_id`),
  CONSTRAINT `FK4go8vtn4msugmpbf6wwkpumkb` FOREIGN KEY (`fornecedor_id`) REFERENCES `fornecedor` (`id`),
  CONSTRAINT `fk_compra_status_transicao` FOREIGN KEY (`status_transicao_id`) REFERENCES `status_carcaca_transicao` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=72 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `compra_auditoria`
--

DROP TABLE IF EXISTS `compra_auditoria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `compra_auditoria` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `detalhes` varchar(2048) DEFAULT NULL,
  `dt_evento` datetime(6) NOT NULL,
  `evento` varchar(50) NOT NULL,
  `compra_id` bigint NOT NULL,
  `usuario_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK6up080ueqanr6dqy2figstl3y` (`compra_id`),
  CONSTRAINT `FK6up080ueqanr6dqy2figstl3y` FOREIGN KEY (`compra_id`) REFERENCES `compra` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=82 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `compra_item`
--

DROP TABLE IF EXISTS `compra_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `compra_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `valor_unitario` decimal(19,2) DEFAULT NULL,
  `carcaca_id` bigint DEFAULT NULL,
  `compra_id` bigint NOT NULL,
  `valor_pago` decimal(19,2) DEFAULT NULL,
  `medida_id` int DEFAULT NULL,
  `codigo` varchar(80) DEFAULT NULL,
  `foto_compra` text,
  `status_transicao_id` int DEFAULT NULL,
  `dt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` datetime DEFAULT NULL,
  `dt_delete` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_compra_item_carcaca` (`carcaca_id`),
  KEY `FKl7w5476vcnmuwxfalmb2srk4` (`compra_id`),
  KEY `FKdx9h3eha7rb62sav3pnpgnl6w` (`medida_id`),
  KEY `fk_compra_item_status_transicao` (`status_transicao_id`),
  CONSTRAINT `fk_compra_item_carcaca` FOREIGN KEY (`carcaca_id`) REFERENCES `carcaca` (`id`),
  CONSTRAINT `fk_compra_item_status_transicao` FOREIGN KEY (`status_transicao_id`) REFERENCES `status_carcaca_transicao` (`id`),
  CONSTRAINT `FKdx9h3eha7rb62sav3pnpgnl6w` FOREIGN KEY (`medida_id`) REFERENCES `medida` (`id`),
  CONSTRAINT `FKl7w5476vcnmuwxfalmb2srk4` FOREIGN KEY (`compra_id`) REFERENCES `compra` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=55 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `compra_item_auditoria`
--

DROP TABLE IF EXISTS `compra_item_auditoria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `compra_item_auditoria` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `detalhes` varchar(2048) DEFAULT NULL,
  `dt_evento` datetime(6) NOT NULL,
  `evento` varchar(50) NOT NULL,
  `compra_item_id` bigint NOT NULL,
  `usuario_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmgndp7jramewc6jv0qa1osqpk` (`compra_item_id`),
  CONSTRAINT `FKmgndp7jramewc6jv0qa1osqpk` FOREIGN KEY (`compra_item_id`) REFERENCES `compra_item` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=52 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `controle_qualidade`
--

DROP TABLE IF EXISTS `controle_qualidade`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `controle_qualidade` (
  `id` int NOT NULL AUTO_INCREMENT,
  `producao_id` int NOT NULL,
  `observacao` varchar(45) DEFAULT NULL,
  `fotos` json NOT NULL,
  `tipo_observacao_id` int NOT NULL,
  `dt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` datetime DEFAULT NULL,
  `dt_delete` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `producao_id_UNIQUE` (`producao_id`),
  KEY `fk_controle_qualidade_producao1_idx` (`producao_id`),
  KEY `fk_controle_qualidade_tipo_observacao1_idx1` (`tipo_observacao_id`),
  CONSTRAINT `fk_controle_qualidade_producao1` FOREIGN KEY (`producao_id`) REFERENCES `producao` (`id`),
  CONSTRAINT `fk_controle_qualidade_tipo_observacao1` FOREIGN KEY (`tipo_observacao_id`) REFERENCES `tipo_observacao` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=46028 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `correcao`
--

DROP TABLE IF EXISTS `correcao`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `correcao` (
  `id` int NOT NULL AUTO_INCREMENT,
  `peso_antes` json NOT NULL,
  `peso_depois` json NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `espessuramento`
--

DROP TABLE IF EXISTS `espessuramento`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `espessuramento` (
  `id` int NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flyway_schema_history_gpcontrol`
--

DROP TABLE IF EXISTS `flyway_schema_history_gpcontrol`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history_gpcontrol` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_gpcontrol_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fornecedor`
--

DROP TABLE IF EXISTS `fornecedor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fornecedor` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nome` varchar(255) NOT NULL,
  `cnpj_cpf` varchar(20) NOT NULL,
  `contato` varchar(255) DEFAULT NULL,
  `apelido` varchar(80) DEFAULT NULL,
  `ativo` bit(1) NOT NULL,
  `cidade` varchar(80) DEFAULT NULL,
  `data_atualizacao` datetime(6) DEFAULT NULL,
  `data_criacao` datetime(6) NOT NULL,
  `estado` varchar(2) DEFAULT NULL,
  `observacoes` text,
  `telefone` varchar(30) DEFAULT NULL,
  `usuario_responsavel_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_fornecedor_cnpj_cpf` (`cnpj_cpf`),
  KEY `fk_fornecedor_usuario_responsavel` (`usuario_responsavel_id`),
  KEY `idx_fornecedor_ativo` (`ativo`),
  KEY `idx_fornecedor_nome` (`nome`),
  KEY `idx_fornecedor_cidade_estado` (`cidade`,`estado`),
  CONSTRAINT `fk_fornecedor_usuario_responsavel` FOREIGN KEY (`usuario_responsavel_id`) REFERENCES `usuario` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `inspecao`
--

DROP TABLE IF EXISTS `inspecao`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inspecao` (
  `id` int NOT NULL AUTO_INCREMENT,
  `checklist` varchar(255) DEFAULT NULL,
  `dt_create` datetime(6) NOT NULL,
  `fotos` varchar(255) DEFAULT NULL,
  `motivos` varchar(1024) DEFAULT NULL,
  `resultado` varchar(255) NOT NULL,
  `carcaca_id` bigint DEFAULT NULL,
  `usuario_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKkigtqab1sufmsiatwg9w0v2h6` (`carcaca_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `localizacao`
--

DROP TABLE IF EXISTS `localizacao`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `localizacao` (
  `id` int NOT NULL AUTO_INCREMENT,
  `descricao` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `maquina_configuracao`
--

DROP TABLE IF EXISTS `maquina_configuracao`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `maquina_configuracao` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `atributos` json DEFAULT NULL,
  `celular_id` varchar(100) NOT NULL,
  `descricao` varchar(150) DEFAULT NULL,
  `dt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_delete` datetime(6) DEFAULT NULL,
  `dt_update` datetime(6) DEFAULT NULL,
  `usuario_id` bigint NOT NULL,
  `maquina_id` bigint NOT NULL,
  `matriz_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqg2omb2o1e1spgu1xixlmkhhs` (`maquina_id`),
  KEY `FK4hgj8hx2bdr45706vwt1v4vve` (`matriz_id`),
  CONSTRAINT `FK4hgj8hx2bdr45706vwt1v4vve` FOREIGN KEY (`matriz_id`) REFERENCES `matriz` (`id`),
  CONSTRAINT `FKqg2omb2o1e1spgu1xixlmkhhs` FOREIGN KEY (`maquina_id`) REFERENCES `maquina_registro` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=124 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `maquina_registro`
--

DROP TABLE IF EXISTS `maquina_registro`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `maquina_registro` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `descricao` varchar(250) DEFAULT NULL,
  `dt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_delete` datetime(6) DEFAULT NULL,
  `dt_update` datetime(6) DEFAULT NULL,
  `nome` varchar(100) NOT NULL,
  `numero_serie` varchar(100) DEFAULT NULL,
  `status` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_s4ma428lpp21oy4y8c11p43pt` (`numero_serie`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `marca`
--

DROP TABLE IF EXISTS `marca`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `marca` (
  `id` int NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=262 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `matriz`
--

DROP TABLE IF EXISTS `matriz`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `matriz` (
  `id` int NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=77 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `medida`
--

DROP TABLE IF EXISTS `medida`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `medida` (
  `id` int NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=59 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `modelo`
--

DROP TABLE IF EXISTS `modelo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `modelo` (
  `id` int NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  `marca_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_modelo_marca1_idx` (`marca_id`),
  CONSTRAINT `fk_modelo_marca1` FOREIGN KEY (`marca_id`) REFERENCES `marca` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1245 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `motorista`
--

DROP TABLE IF EXISTS `motorista`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `motorista` (
  `id` int NOT NULL AUTO_INCREMENT,
  `ativo` bit(1) NOT NULL,
  `cpf` varchar(11) NOT NULL,
  `data_atualizacao` datetime(6) DEFAULT NULL,
  `data_criacao` datetime(6) DEFAULT NULL,
  `nome` varchar(255) NOT NULL,
  `observacoes` varchar(1024) DEFAULT NULL,
  `placa_veiculo` varchar(255) DEFAULT NULL,
  `telefone` varchar(255) DEFAULT NULL,
  `usuario_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_m3qo604s7ko66puhowap8mohy` (`usuario_id`),
  UNIQUE KEY `UK_rbjk7fv6x6kadmtchy9pb5bt3` (`cpf`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pais`
--

DROP TABLE IF EXISTS `pais`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pais` (
  `id` int NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `perfil`
--

DROP TABLE IF EXISTS `perfil`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `perfil` (
  `id` int NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pneus_vulcanizados`
--

DROP TABLE IF EXISTS `pneus_vulcanizados`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pneus_vulcanizados` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dt_create` datetime(6) NOT NULL,
  `dt_delete` datetime(6) DEFAULT NULL,
  `dt_update` datetime(6) DEFAULT NULL,
  `producao_id` bigint NOT NULL,
  `status` varchar(255) NOT NULL,
  `usuario_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `producao`
--

DROP TABLE IF EXISTS `producao`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `producao` (
  `id` int NOT NULL AUTO_INCREMENT,
  `carcaca_id` bigint DEFAULT NULL,
  `medida_pneu_raspado` decimal(4,3) NOT NULL,
  `dados` json NOT NULL,
  `regra_id` int NOT NULL,
  `fotos` json DEFAULT NULL,
  `dt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` datetime DEFAULT NULL,
  `dt_delete` datetime DEFAULT NULL,
  `uuid` binary(16) DEFAULT NULL,
  `usuario_id` int NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `fk_producao_pneu1_idx` (`carcaca_id`),
  KEY `fk_producao_regra1_idx` (`regra_id`),
  KEY `fk_producao_usuario1_idx` (`usuario_id`),
  CONSTRAINT `fk_producao_pneu1` FOREIGN KEY (`carcaca_id`) REFERENCES `carcaca` (`id`),
  CONSTRAINT `fk_producao_regra1` FOREIGN KEY (`regra_id`) REFERENCES `regra` (`id`),
  CONSTRAINT `fk_producao_usuario1` FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=64079 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `regra`
--

DROP TABLE IF EXISTS `regra`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `regra` (
  `id` int NOT NULL AUTO_INCREMENT,
  `tamanho_min` decimal(4,3) NOT NULL,
  `tamanho_max` decimal(4,3) NOT NULL,
  `tempo` varchar(45) DEFAULT NULL,
  `matriz_id` int NOT NULL,
  `medida_id` int NOT NULL,
  `pais_id` int NOT NULL,
  `modelo_id` int NOT NULL,
  `camelback_id` int NOT NULL,
  `espessuramento_id` int DEFAULT NULL,
  `antiquebra1_id` int NOT NULL,
  `antiquebra2_id` int DEFAULT NULL,
  `antiquebra3_id` int DEFAULT NULL,
  `dt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` datetime DEFAULT NULL,
  `dt_delete` datetime DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
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
) ENGINE=InnoDB AUTO_INCREMENT=5446 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rele`
--

DROP TABLE IF EXISTS `rele`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rele` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `celular_id` varchar(100) NOT NULL,
  `dt_create` datetime(6) NOT NULL,
  `dt_delete` datetime(6) DEFAULT NULL,
  `dt_update` datetime(6) DEFAULT NULL,
  `ip` varchar(255) NOT NULL,
  `maquina_registro_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_8juy58eud5mns037hl6f9rr7m` (`ip`),
  KEY `FKj9xaqqh8egsgvd7rnd2dthyac` (`maquina_registro_id`),
  CONSTRAINT `FKj9xaqqh8egsgvd7rnd2dthyac` FOREIGN KEY (`maquina_registro_id`) REFERENCES `maquina_registro` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `status_carcaca`
--

DROP TABLE IF EXISTS `status_carcaca`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `status_carcaca` (
  `id` int NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `codigo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `status_carcaca_transicao`
--

DROP TABLE IF EXISTS `status_carcaca_transicao`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `status_carcaca_transicao` (
  `id` int NOT NULL AUTO_INCREMENT,
  `codigo` varchar(255) DEFAULT NULL,
  `descricao` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=82 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tipo_classificacao`
--

DROP TABLE IF EXISTS `tipo_classificacao`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tipo_classificacao` (
  `id` int NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tipo_observacao`
--

DROP TABLE IF EXISTS `tipo_observacao`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tipo_observacao` (
  `id` int NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) NOT NULL,
  `tipo_classificacao_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_tipo_observacao_tipo_classificacao1_idx` (`tipo_classificacao_id`),
  CONSTRAINT `fk_tipo_observacao_tipo_classificacao1` FOREIGN KEY (`tipo_classificacao_id`) REFERENCES `tipo_classificacao` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tipo_valida_regra`
--

DROP TABLE IF EXISTS `tipo_valida_regra`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tipo_valida_regra` (
  `id` int NOT NULL AUTO_INCREMENT,
  `descricao` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `usuario`
--

DROP TABLE IF EXISTS `usuario`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuario` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nome` varchar(45) DEFAULT NULL,
  `login` varchar(45) DEFAULT NULL,
  `senha` varchar(250) DEFAULT NULL,
  `ativo` bit(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `usuario_perfil`
--

DROP TABLE IF EXISTS `usuario_perfil`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuario_perfil` (
  `id` int NOT NULL AUTO_INCREMENT,
  `perfil_id` int NOT NULL,
  `usuario_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_usuario_perfil_perfil1_idx` (`perfil_id`),
  KEY `fk_usuario_perfil_usuario1_idx` (`usuario_id`),
  CONSTRAINT `fk_usuario_perfil_perfil1` FOREIGN KEY (`perfil_id`) REFERENCES `perfil` (`id`),
  CONSTRAINT `fk_usuario_perfil_usuario1` FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `valida_regra`
--

DROP TABLE IF EXISTS `valida_regra`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `valida_regra` (
  `id` int NOT NULL AUTO_INCREMENT,
  `dados` varchar(255) DEFAULT NULL,
  `status` bit(1) DEFAULT NULL,
  `qualidade_id` int DEFAULT NULL,
  `regra_id` int DEFAULT NULL,
  `tipo_valida_regra_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK4hflho3eiq4tpsfudl6fku0fy` (`qualidade_id`),
  KEY `FKgvqilhcd5705umlsk29bu2rl6` (`regra_id`),
  KEY `FKlrl8o4x3hksl47utc8ftdcah6` (`tipo_valida_regra_id`),
  CONSTRAINT `FK4hflho3eiq4tpsfudl6fku0fy` FOREIGN KEY (`qualidade_id`) REFERENCES `controle_qualidade` (`id`),
  CONSTRAINT `FKgvqilhcd5705umlsk29bu2rl6` FOREIGN KEY (`regra_id`) REFERENCES `regra` (`id`),
  CONSTRAINT `FKlrl8o4x3hksl47utc8ftdcah6` FOREIGN KEY (`tipo_valida_regra_id`) REFERENCES `tipo_valida_regra` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3971 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `venda`
--

DROP TABLE IF EXISTS `venda`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `venda` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `data_venda` datetime(6) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `valor_total` decimal(19,2) DEFAULT NULL,
  `cliente_id` int DEFAULT NULL,
  `usuario_id` bigint DEFAULT NULL,
  `status_transicao_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK50murhuotq9h2dnxej317jjiy` (`cliente_id`),
  KEY `fk_venda_status_transicao` (`status_transicao_id`),
  CONSTRAINT `FK50murhuotq9h2dnxej317jjiy` FOREIGN KEY (`cliente_id`) REFERENCES `cliente` (`id`),
  CONSTRAINT `fk_venda_status_transicao` FOREIGN KEY (`status_transicao_id`) REFERENCES `status_carcaca_transicao` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `venda_item`
--

DROP TABLE IF EXISTS `venda_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `venda_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `valor` decimal(19,2) DEFAULT NULL,
  `carcaca_id` bigint DEFAULT NULL,
  `venda_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKtkprll67vcsr8awv6a83cfdl5` (`carcaca_id`),
  KEY `FKccx5r1i1laij7vugru0kdhkhe` (`venda_id`),
  CONSTRAINT `FKccx5r1i1laij7vugru0kdhkhe` FOREIGN KEY (`venda_id`) REFERENCES `venda` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-06 22:38:44
