-- MySQL Workbench Synchronization
-- Generated: 2022-09-09 22:20
-- Model: Sakila Full
-- Version: 2.0
-- Project: Name of the project
-- Author: Mike

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

CREATE TABLE IF NOT EXISTS `gppremium`.`carcaca_rejeitada` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `modelo_id` INT(11) NOT NULL,
  `medida_id` INT(11) NOT NULL,
  `pais_id` INT(11) NOT NULL,
  `dt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dt_update` DATETIME NULL DEFAULT NULL,
  `dt_delete` DATETIME NULL DEFAULT NULL,
  `uuid` BINARY(16) NULL DEFAULT NULL,
  `motivo` VARCHAR(250) NULL DEFAULT NULL,
  `descricao` VARCHAR(250) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_pneu_modelo1_idx` (`modelo_id` ASC) VISIBLE,
  INDEX `fk_pneu_medida1_idx` (`medida_id` ASC) VISIBLE,
  INDEX `fk_pneu_pais1_idx` (`pais_id` ASC) VISIBLE,
  CONSTRAINT `fk_pneu_medida10`
    FOREIGN KEY (`medida_id`)
    REFERENCES `gppremium`.`medida` (`id`),
  CONSTRAINT `fk_pneu_modelo10`
    FOREIGN KEY (`modelo_id`)
    REFERENCES `gppremium`.`modelo` (`id`),
  CONSTRAINT `fk_pneu_pais10`
    FOREIGN KEY (`pais_id`)
    REFERENCES `gppremium`.`pais` (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 2442
DEFAULT CHARACTER SET = utf8;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
