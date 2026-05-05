-- MySQL dump 10.13  Distrib 8.0.43, for macos15 (arm64)
--
-- Host: 127.0.0.1    Database: gppremium
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
-- Table structure for table `maquina_configuracao`
--

DROP TABLE IF EXISTS `maquina_configuracao`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `maquina_configuracao` (
  `id` int NOT NULL AUTO_INCREMENT,
  `celular_id` varchar(100) NOT NULL,
  `descricao` varchar(150) DEFAULT NULL,
  `atributos` json DEFAULT NULL,
  `dt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `matriz_id` int NOT NULL,
  `maquina_id` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_celular` (`celular_id`),
  KEY `fk_maquina_config_matriz_idx` (`matriz_id`),
  KEY `fk_maquina_configuracao_maquina1_idx` (`maquina_id`),
  CONSTRAINT `fk_maquina_config_matriz` FOREIGN KEY (`matriz_id`) REFERENCES `matriz` (`id`),
  CONSTRAINT `fk_maquina_configuracao_maquina1` FOREIGN KEY (`maquina_id`) REFERENCES `maquina_registro` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `maquina_configuracao`
--

LOCK TABLES `maquina_configuracao` WRITE;
/*!40000 ALTER TABLE `maquina_configuracao` DISABLE KEYS */;
/*!40000 ALTER TABLE `maquina_configuracao` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `maquina_registro`
--

DROP TABLE IF EXISTS `maquina_registro`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `maquina_registro` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nome` varchar(100) NOT NULL,
  `numero_serie` varchar(100) DEFAULT NULL,
  `status` enum('Ativa','Inativa','Manutencao') DEFAULT 'Ativa',
  `descricao` varchar(250) DEFAULT NULL,
  `dt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `dt_delete` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `numero_serie` (`numero_serie`),
  KEY `idx_dt_delete` (`dt_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `maquina_registro`
--

LOCK TABLES `maquina_registro` WRITE;
/*!40000 ALTER TABLE `maquina_registro` DISABLE KEYS */;
/*!40000 ALTER TABLE `maquina_registro` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-09-28 20:14:07
