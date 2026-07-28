USE `ConfiancaManger`;

-- Remove campos de vigencia que nao sao usados no cadastro/consulta de reembolso.
-- A regra passa a ser controlada por ativo/status_revisao.

DROP PROCEDURE IF EXISTS `sp_drop_column_if_exists`;

DELIMITER $$

CREATE PROCEDURE `sp_drop_column_if_exists`(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = p_table_name
           AND COLUMN_NAME = p_column_name
    ) THEN
        SET @sql_stmt = CONCAT('ALTER TABLE `', p_table_name, '` DROP COLUMN `', p_column_name, '`');
        PREPARE stmt FROM @sql_stmt;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL `sp_drop_column_if_exists`('regra_aerea_reembolso', 'vigencia_inicio');
CALL `sp_drop_column_if_exists`('regra_aerea_reembolso', 'vigencia_fim');

DROP PROCEDURE IF EXISTS `sp_drop_column_if_exists`;
