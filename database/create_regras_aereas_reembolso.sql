USE `ConfiancaManger`;

-- Reembolso: tabela principal por familia cadastrada, classe e momento.
-- Objetivo: responder se a reserva permite reembolso e calcular uma previa com base nos valores da reserva.
CREATE TABLE IF NOT EXISTS `regra_aerea_reembolso` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `codg_familia_companhia` INT NULL,

  `classes_reserva` VARCHAR(255) NULL,
  `classes_reserva_json` JSON NULL,
  `momento` VARCHAR(40) NOT NULL,

  `permite_reembolso` TINYINT(1) NOT NULL DEFAULT 0,
  `status_reembolso` VARCHAR(60) NOT NULL,
  `tipo_reembolso` VARCHAR(60) NOT NULL,
  `reembolsa_tarifa` TINYINT(1) NOT NULL DEFAULT 0,
  `reembolsa_taxa_embarque` TINYINT(1) NOT NULL DEFAULT 1,
  `requer_simulacao` TINYINT(1) NOT NULL DEFAULT 0,
  `requer_validacao_manual` TINYINT(1) NOT NULL DEFAULT 0,

  `aplica_multa` TINYINT(1) NOT NULL DEFAULT 0,
  `tipo_multa` VARCHAR(60) NOT NULL DEFAULT 'SEM_MULTA',
  `moeda_multa` VARCHAR(10) NULL,
  `valor_multa_fixo` DECIMAL(15,2) NULL,
  `percentual_multa` DECIMAL(8,2) NULL,
  `percentual_reembolso` DECIMAL(8,2) NULL,
  `base_calculo_multa` VARCHAR(80) NULL,
  `por_passageiro` TINYINT(1) NOT NULL DEFAULT 1,
  `por_trecho` TINYINT(1) NOT NULL DEFAULT 0,

  `considerar_tarifa` TINYINT(1) NOT NULL DEFAULT 1,
  `considerar_taxa_embarque` TINYINT(1) NOT NULL DEFAULT 1,
  `considerar_taxa_du` TINYINT(1) NOT NULL DEFAULT 0,
  `considerar_rav` TINYINT(1) NOT NULL DEFAULT 0,
  `considerar_rc` TINYINT(1) NOT NULL DEFAULT 0,
  `considerar_taxa_assento` TINYINT(1) NOT NULL DEFAULT 0,
  `considerar_taxa_bagagem` TINYINT(1) NOT NULL DEFAULT 0,
  `considerar_outras_taxas` TINYINT(1) NOT NULL DEFAULT 0,

  `titulo_usuario` VARCHAR(160) NULL,
  `descricao_usuario` TEXT NULL,
  `mensagem_calculo` TEXT NULL,
  `orientacao_interna` TEXT NULL,
  `observacao` TEXT NULL,

  `fonte_url` TEXT NULL,
  `data_consulta` DATETIME NULL,
  `prioridade` INT NOT NULL DEFAULT 100,
  `origem_regra` VARCHAR(40) NOT NULL DEFAULT 'IA',
  `status_revisao` VARCHAR(40) NOT NULL DEFAULT 'PENDENTE_REVISAO',
  `ativo` TINYINT(1) NOT NULL DEFAULT 1,
  `data_criacao` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `data_atualizacao` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  KEY `idx_reembolso_familia_companhia` (`codg_familia_companhia`),
  KEY `idx_reembolso_match` (`codg_familia_companhia`, `momento`, `ativo`, `prioridade`),
  KEY `idx_reembolso_classe` (`classes_reserva`, `momento`, `ativo`, `prioridade`),
  KEY `idx_reembolso_status` (`status_reembolso`, `ativo`),
  KEY `idx_reembolso_revisao` (`status_revisao`, `ativo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Alias para casar a familia/classe que vem da reserva com a familia normalizada cadastrada.
-- Exemplo: Azul "Mais Azul" / "+AZ" -> "NAO_REEMBOLSAVEL".
CREATE TABLE IF NOT EXISTS `regra_aerea_reembolso_alias` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `companhia` VARCHAR(20) NOT NULL,
  `sistema_origem` VARCHAR(60) NULL,
  `mercado` VARCHAR(60) NULL,
  `cabine` VARCHAR(60) NULL,
  `familia_origem` VARCHAR(120) NULL,
  `familia_codigo_origem` VARCHAR(60) NULL,
  `classe_reserva` VARCHAR(40) NULL,
  `familia_tarifaria_normalizada` VARCHAR(100) NOT NULL,
  `prioridade` INT NOT NULL DEFAULT 100,
  `observacao` TEXT NULL,
  `ativo` TINYINT(1) NOT NULL DEFAULT 1,
  `data_criacao` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `data_atualizacao` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  KEY `idx_reembolso_alias_match` (`companhia`, `sistema_origem`, `familia_origem`, `familia_codigo_origem`, `classe_reserva`, `ativo`, `prioridade`),
  KEY `idx_reembolso_alias_normalizada` (`companhia`, `familia_tarifaria_normalizada`, `ativo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Valores adicionais quando a regra tiver canal, moeda ou regiao diferente.
-- Mantem o cadastro manual sem depender de JSON para taxas fixas alternativas.
CREATE TABLE IF NOT EXISTS `regra_aerea_reembolso_valor` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `regra_aerea_reembolso_id` BIGINT NOT NULL,
  `tipo_valor` VARCHAR(60) NOT NULL,
  `canal` VARCHAR(60) NULL,
  `mercado_regiao` VARCHAR(80) NULL,
  `moeda` VARCHAR(10) NULL,
  `valor` DECIMAL(15,2) NULL,
  `percentual` DECIMAL(8,2) NULL,
  `observacao` TEXT NULL,
  `ativo` TINYINT(1) NOT NULL DEFAULT 1,
  `data_criacao` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `data_atualizacao` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  KEY `idx_reembolso_valor_regra` (`regra_aerea_reembolso_id`, `ativo`),
  KEY `idx_reembolso_valor_tipo` (`tipo_valor`, `canal`, `moeda`, `ativo`),
  CONSTRAINT `fk_reembolso_valor_regra`
    FOREIGN KEY (`regra_aerea_reembolso_id`)
    REFERENCES `regra_aerea_reembolso` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
