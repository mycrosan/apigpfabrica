-- MySQL Workbench Synchronization
-- Generated: 2025-08-27 00:27
-- Model: New Model
-- Version: 1.0
-- Project: Name of the project
-- Author: my

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

ALTER TABLE `gppremium`.`cobertura` 
DROP FOREIGN KEY `fk_cobertura_producao1`;

ALTER TABLE `gppremium`.`cobertura` 
ADD COLUMN `cola_id` INT(11) NOT NULL AFTER `usuario_id`,
CHANGE COLUMN `producao_id` `producao_id` INT(11) NULL DEFAULT NULL ,
ADD INDEX `fk_cobertura_cola1_idx` (`cola_id` ASC) VISIBLE;
;

ALTER TABLE `gppremium`.`cobertura` 
ADD CONSTRAINT `fk_cobertura_producao1`
  FOREIGN KEY (`producao_id`)
  REFERENCES `gppremium`.`producao` (`id`),
ADD CONSTRAINT `fk_cobertura_cola1`
  FOREIGN KEY (`cola_id`)
  REFERENCES `gppremium`.`cola` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

-- Atualizar cobertura com as colas
UPDATE cobertura c
JOIN cola co ON c.producao_id = co.producao_id
SET c.cola_id = co.id
WHERE c.cola_id = 0;


select * from cobertura where cola_id = 0

SET SQL_SAFE_UPDATES = 0;

DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '18');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '19');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '20');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '21');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '22');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '23');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '24');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '25');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '26');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '27');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '28');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '29');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '30');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '31');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '32');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '33');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '34');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '35');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '36');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '37');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '38');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '39');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '40');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '41');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '42');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '43');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '44');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '45');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '46');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '47');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '48');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '49');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '50');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '51');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '52');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '53');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '54');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '55');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '56');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '57');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '58');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '59');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '60');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '61');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '62');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '63');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '64');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '65');
DELETE FROM `gppremium`.`cobertura` WHERE (`id` = '66');