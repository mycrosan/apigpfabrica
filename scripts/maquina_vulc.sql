-- MySQL Workbench Synchronization
-- Generated: 2025-10-15 09:34
-- Model: New Model
-- Version: 1.0
-- Project: Name of the project
-- Author: Sandy Santos

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

CREATE TABLE IF NOT EXISTS `gppremium`.`maquina_configuracao` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `celular_id` VARCHAR(100) NOT NULL,
  `descricao` VARCHAR(150) NULL DEFAULT NULL,
  `atributos` JSON NULL DEFAULT NULL,
  `matriz_id` INT(11) NOT NULL,
  `maquina_id` INT(11) NOT NULL,
  `usuario_id` INT(11) NOT NULL,
  `dt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` DATETIME NULL DEFAULT NULL,
  `dt_delete` DATETIME NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_maquina_config_matriz_idx` (`matriz_id` ASC) VISIBLE,
  INDEX `fk_maquina_configuracao_maquina1_idx` (`maquina_id` ASC) VISIBLE,
  INDEX `fk_maquina_configuracao_usuario1_idx` (`usuario_id` ASC) VISIBLE,
  INDEX `idx_celular_dt_create` (`celular_id` ASC, `dt_create` DESC) VISIBLE,
  CONSTRAINT `fk_maquina_config_matriz`
    FOREIGN KEY (`matriz_id`)
    REFERENCES `gppremium`.`matriz` (`id`),
  CONSTRAINT `fk_maquina_configuracao_maquina1`
    FOREIGN KEY (`maquina_id`)
    REFERENCES `gppremium`.`maquina_registro` (`id`),
  CONSTRAINT `fk_maquina_configuracao_usuario1`
    FOREIGN KEY (`usuario_id`)
    REFERENCES `gppremium`.`usuario` (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 32
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
COMMENT = 'Configurações de máquina por celular - permite múltiplas configurações, sendo a mais recente a ativa';

CREATE TABLE IF NOT EXISTS `gppremium`.`maquina_registro` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `nome` VARCHAR(100) NOT NULL,
  `numero_serie` VARCHAR(100) NULL DEFAULT NULL,
  `status` ENUM('Ativa', 'Inativa', 'Manutencao') NULL DEFAULT 'Ativa',
  `descricao` VARCHAR(250) NULL DEFAULT NULL,
  `dt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` DATETIME NULL DEFAULT NULL,
  `dt_delete` DATETIME NULL DEFAULT NULL,
  `usuario_id` INT(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `numero_serie` (`numero_serie` ASC) VISIBLE,
  INDEX `idx_dt_delete` (`dt_delete` ASC) VISIBLE,
  INDEX `idx_usuario_id` (`usuario_id` ASC) VISIBLE,
  CONSTRAINT `fk_maquina_registro_usuario`
    FOREIGN KEY (`usuario_id`)
    REFERENCES `gppremium`.`usuario` (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 7
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `gppremium`.`pneus_vulcanizados` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `dt_create` DATETIME NOT NULL,
  `dt_delete` DATETIME NULL DEFAULT NULL,
  `dt_update` DATETIME NULL DEFAULT NULL,
  `status` VARCHAR(255) NOT NULL,
  `usuario_id` INT(11) NOT NULL,
  `producao_id` INT(11) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_pneus_vulcanizados_usuario1_idx` (`usuario_id` ASC) VISIBLE,
  INDEX `fk_pneus_vulcanizados_producao1_idx` (`producao_id` ASC) VISIBLE,
  INDEX `idx_usuario_id` (`usuario_id` ASC) VISIBLE,
  INDEX `idx_producao_id` (`producao_id` ASC) VISIBLE,
  INDEX `idx_status` (`status` ASC) VISIBLE,
  INDEX `idx_dt_create` (`dt_create` ASC) VISIBLE,
  INDEX `idx_dt_delete` (`dt_delete` ASC) VISIBLE,
  CONSTRAINT `fk_pneus_vulcanizados_producao1`
    FOREIGN KEY (`producao_id`)
    REFERENCES `gppremium`.`producao` (`id`),
  CONSTRAINT `fk_pneus_vulcanizados_usuario1`
    FOREIGN KEY (`usuario_id`)
    REFERENCES `gppremium`.`usuario` (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 20
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
COMMENT = 'Tabela para controle de pneus vulcanizados por usuário e produção';

CREATE TABLE IF NOT EXISTS `gppremium`.`rele` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `dt_create` DATETIME NOT NULL,
  `dt_delete` DATETIME NULL DEFAULT NULL,
  `dt_update` DATETIME NULL DEFAULT NULL,
  `ip` VARCHAR(255) NOT NULL,
  `celular_id` VARCHAR(100) NOT NULL,
  `maquina_registro_id` INT(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `UK_r1x9koe3tgc0ebaojyd45ip9b` (`ip` ASC) VISIBLE,
  INDEX `fk_rele_maquina_registro1_idx` (`maquina_registro_id` ASC) VISIBLE,
  CONSTRAINT `fk_rele_maquina_registro`
    FOREIGN KEY (`maquina_registro_id`)
    REFERENCES `gppremium`.`maquina_registro` (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 7
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `gppremium`.`tipo_valida_regra` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `descricao` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `gppremium`.`valida_regra` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `dados` VARCHAR(255) NULL DEFAULT NULL,
  `status` BIT(1) NULL DEFAULT NULL,
  `qualidade_id` INT(11) NULL DEFAULT NULL,
  `regra_id` INT(11) NULL DEFAULT NULL,
  `tipo_valida_regra_id` INT(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `FK4hflho3eiq4tpsfudl6fku0fy` (`qualidade_id` ASC) VISIBLE,
  INDEX `FKgvqilhcd5705umlsk29bu2rl6` (`regra_id` ASC) VISIBLE,
  INDEX `FKlrl8o4x3hksl47utc8ftdcah6` (`tipo_valida_regra_id` ASC) VISIBLE,
  CONSTRAINT `FK4hflho3eiq4tpsfudl6fku0fy`
    FOREIGN KEY (`qualidade_id`)
    REFERENCES `gppremium`.`controle_qualidade` (`id`),
  CONSTRAINT `FKgvqilhcd5705umlsk29bu2rl6`
    FOREIGN KEY (`regra_id`)
    REFERENCES `gppremium`.`regra` (`id`),
  CONSTRAINT `FKlrl8o4x3hksl47utc8ftdcah6`
    FOREIGN KEY (`tipo_valida_regra_id`)
    REFERENCES `gppremium`.`tipo_valida_regra` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
