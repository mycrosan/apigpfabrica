-- MySQL Workbench Synchronization
-- Generated: 2025-08-17 19:00
-- Model: New Model
-- Version: 1.0
-- Project: Name of the project
-- Author: my

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

ALTER TABLE `gppremium`.`cobertura`
ADD COLUMN `usuario_id` INT(11) NOT NULL AFTER `dt_delete`,
ADD INDEX `fk_cobertura_usuario1_idx` (`usuario_id` ASC) VISIBLE;
;

CREATE TABLE IF NOT EXISTS `gppremium`.`cola` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `producao_id` INT(11) NOT NULL,
  `data_inicio` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `status` ENUM('Aguardando', 'Pronto', 'Vencido') NULL DEFAULT 'Aguardando',
  `dt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` DATETIME NULL DEFAULT NULL,
  `usuario_id` INT(11) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_cola_producao` (`producao_id` ASC) VISIBLE,
  INDEX `fk_cola_usuario1_idx` (`usuario_id` ASC) VISIBLE,
  CONSTRAINT `fk_cola_producao`
    FOREIGN KEY (`producao_id`)
    REFERENCES `gppremium`.`producao` (`id`),
  CONSTRAINT `fk_cola_usuario1`
    FOREIGN KEY (`usuario_id`)
    REFERENCES `gppremium`.`usuario` (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 31
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

ALTER TABLE `gppremium`.`cobertura`
ADD CONSTRAINT `fk_cobertura_usuario1`
  FOREIGN KEY (`usuario_id`)
  REFERENCES `gppremium`.`usuario` (`id`);


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
