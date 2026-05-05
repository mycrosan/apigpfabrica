SELECT * FROM fluxo_familiar.lancamento;-- MySQL Workbench Synchronization
-- Generated: 2025-08-11 09:11
-- Model: New Model
-- Version: 1.0
-- Project: Name of the project
-- Author: my

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

ALTER TABLE `gppremium`.`cobertura` 
DROP FOREIGN KEY `fk_cobertura_usuario1`;

ALTER TABLE `gppremium`.`cola` 
ADD COLUMN `usuario_id` INT(11) NOT NULL AFTER `dt_update`,
ADD INDEX `fk_cola_usuario1_idx` (`usuario_id` ASC) VISIBLE;
;

ALTER TABLE `gppremium`.`cobertura` 
ADD CONSTRAINT `fk_cobertura_usuario1`
  FOREIGN KEY (`usuario_id`)
  REFERENCES `gppremium`.`usuario` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

ALTER TABLE `gppremium`.`cola` 
ADD CONSTRAINT `fk_cola_usuario1`
  FOREIGN KEY (`usuario_id`)
  REFERENCES `gppremium`.`usuario` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
