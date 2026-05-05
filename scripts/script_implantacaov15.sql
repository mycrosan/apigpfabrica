-- MySQL Workbench Synchronization
-- Generated: 2022-05-13 22:29
-- Model: New Model
-- Version: 1.0
-- Project: Name of the project
-- Author: Sandy

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

ALTER SCHEMA `gppremium`  DEFAULT CHARACTER SET utf8mb4  DEFAULT COLLATE utf8mb4_unicode_ci ;

ALTER TABLE `gppremium`.`carcaca` 
ADD COLUMN `status_carcaca_id` INT(11) NULL DEFAULT NULL AFTER `fotos`,
ADD COLUMN `uuid` BINARY(16) NULL DEFAULT NULL AFTER `dt_delete`,
ADD INDEX `fk_carcaca_status_carcaca1_idx` (`status_carcaca_id` ASC);
;

ALTER TABLE `gppremium`.`controle_qualidade`
CHANGE COLUMN `tipo_classificacao_id` `tipo_classificacao_id` INT(11) NOT NULL COMMENT 'NET\\nLOJA\\nLOJA C\\nD\\nESCARIAR\\nDESCARTE' ,
CHANGE COLUMN `tipo_observacao_id` `tipo_observacao_id` INT(11) NOT NULL COMMENT 'BOLHA POR DENTRO\\nFALHA NA ANTIQUEBRA\\nTALAO FROUXO\\nBOLHA EXTERNA QDO ESTAVA QUENTE' ;

ALTER TABLE `gppremium`.`credenciados`
CHARACTER SET = utf8mb4 , COLLATE = utf8mb4_unicode_ci ;

DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

ALTER TABLE `gppremium`.`producao` 
ADD COLUMN `uuid` BINARY(16) NULL DEFAULT NULL AFTER `dt_delete`;

CREATE TABLE IF NOT EXISTS `gppremium`.`status_carcaca` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `descricao` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

ALTER TABLE `gppremium`.`carcaca` 
ADD CONSTRAINT `fk_carcaca_status_carcaca1`
  FOREIGN KEY (`status_carcaca_id`)
  REFERENCES `gppremium`.`status_carcaca` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
