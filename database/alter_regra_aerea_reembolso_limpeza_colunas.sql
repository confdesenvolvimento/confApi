USE `ConfiancaManger`;

-- Limpa colunas duplicadas na tabela principal de reembolso.
-- A companhia, mercado, familia e codigo tarifario passam a vir de familia_companhia.
-- Este script nao cria FK nova para evitar impacto em bases existentes.
--
-- Antes de executar, valide se o de/para ja foi feito:
-- SELECT id, classes_reserva, momento
--   FROM regra_aerea_reembolso
--  WHERE codg_familia_companhia IS NULL;
-- Regras propositalmente apenas por classe podem continuar sem familia.

DROP PROCEDURE IF EXISTS `sp_drop_fk_if_exists`;
DROP PROCEDURE IF EXISTS `sp_drop_index_if_exists`;
DROP PROCEDURE IF EXISTS `sp_drop_column_if_exists`;
DROP PROCEDURE IF EXISTS `sp_create_index_if_not_exists`;

DELIMITER $$

CREATE PROCEDURE `sp_drop_fk_if_exists`(
    IN p_table_name VARCHAR(64),
    IN p_constraint_name VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1
          FROM information_schema.TABLE_CONSTRAINTS
         WHERE CONSTRAINT_SCHEMA = DATABASE()
           AND TABLE_NAME = p_table_name
           AND CONSTRAINT_NAME = p_constraint_name
           AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ) THEN
        SET @sql_stmt = CONCAT('ALTER TABLE `', p_table_name, '` DROP FOREIGN KEY `', p_constraint_name, '`');
        PREPARE stmt FROM @sql_stmt;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE `sp_drop_index_if_exists`(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1
          FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = p_table_name
           AND INDEX_NAME = p_index_name
    ) THEN
        SET @sql_stmt = CONCAT('DROP INDEX `', p_index_name, '` ON `', p_table_name, '`');
        PREPARE stmt FROM @sql_stmt;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

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

CREATE PROCEDURE `sp_create_index_if_not_exists`(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_index_sql TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = p_table_name
           AND INDEX_NAME = p_index_name
    ) THEN
        SET @sql_stmt = p_index_sql;
        PREPARE stmt FROM @sql_stmt;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL `sp_drop_fk_if_exists`('regra_aerea_reembolso', 'fk_reembolso_versao');

CALL `sp_drop_index_if_exists`('regra_aerea_reembolso', 'idx_reembolso_versao');
CALL `sp_drop_index_if_exists`('regra_aerea_reembolso', 'idx_reembolso_match');
CALL `sp_drop_index_if_exists`('regra_aerea_reembolso', 'idx_reembolso_familia');

CALL `sp_drop_column_if_exists`('regra_aerea_reembolso', 'regra_aerea_versao_id');
CALL `sp_drop_column_if_exists`('regra_aerea_reembolso', 'companhia');
CALL `sp_drop_column_if_exists`('regra_aerea_reembolso', 'nome_companhia');
CALL `sp_drop_column_if_exists`('regra_aerea_reembolso', 'mercado');
CALL `sp_drop_column_if_exists`('regra_aerea_reembolso', 'cabine');
CALL `sp_drop_column_if_exists`('regra_aerea_reembolso', 'familia_tarifaria');
CALL `sp_drop_column_if_exists`('regra_aerea_reembolso', 'familia_tarifaria_normalizada');
CALL `sp_drop_column_if_exists`('regra_aerea_reembolso', 'codigo_tarifario');
CALL `sp_drop_column_if_exists`('regra_aerea_reembolso', 'vigencia_inicio');
CALL `sp_drop_column_if_exists`('regra_aerea_reembolso', 'vigencia_fim');
CALL `sp_drop_column_if_exists`('regra_aerea_reembolso', 'raw_json_item');

CALL `sp_create_index_if_not_exists`(
    'regra_aerea_reembolso',
    'idx_reembolso_familia_companhia',
    'CREATE INDEX `idx_reembolso_familia_companhia` ON `regra_aerea_reembolso` (`codg_familia_companhia`)'
);

CALL `sp_create_index_if_not_exists`(
    'regra_aerea_reembolso',
    'idx_reembolso_match',
    'CREATE INDEX `idx_reembolso_match` ON `regra_aerea_reembolso` (`codg_familia_companhia`, `momento`, `ativo`, `prioridade`)'
);

CALL `sp_create_index_if_not_exists`(
    'regra_aerea_reembolso',
    'idx_reembolso_classe',
    'CREATE INDEX `idx_reembolso_classe` ON `regra_aerea_reembolso` (`classes_reserva`, `momento`, `ativo`, `prioridade`)'
);

DROP PROCEDURE IF EXISTS `sp_drop_fk_if_exists`;
DROP PROCEDURE IF EXISTS `sp_drop_index_if_exists`;
DROP PROCEDURE IF EXISTS `sp_drop_column_if_exists`;
DROP PROCEDURE IF EXISTS `sp_create_index_if_not_exists`;
