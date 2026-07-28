USE `ConfiancaManger`;

-- Adiciona o campo de referencia para familia_companhia.
-- Importante: este script NAO cria FK fisica, para evitar impacto em producao.
ALTER TABLE `regra_aerea_reembolso`
  ADD COLUMN `codg_familia_companhia` INT NULL AFTER `regra_aerea_versao_id`;

CREATE INDEX `idx_reembolso_familia_companhia`
  ON `regra_aerea_reembolso` (`codg_familia_companhia`);

-- Preenche a referencia quando for possivel casar:
-- - regra_aerea_reembolso.codigo_tarifario -> familia_companhia.cod_sigla
-- - regra_aerea_reembolso.mercado -> familia_companhia.tipo_rota
--   0 = ambos, 1 = nacional, 2 = internacional
-- - regra_aerea_reembolso.companhia -> companhia_aerea.iata_cia/nome_cia
--
-- Observacao: os campos legados continuam na tabela por enquanto.
-- A remocao fisica deve ser uma segunda etapa, depois de validar producao.
UPDATE `regra_aerea_reembolso` rar
SET rar.`codg_familia_companhia` = (
  SELECT fc.`codgFamiliaCompanhia`
  FROM `familia_companhia` fc
  INNER JOIN `companhia_aerea` ca
    ON ca.`codg_companhia_aerea` = fc.`codg_companhia_aerea`
  WHERE
    (
      fc.`tipo_rota` = 0
      OR fc.`tipo_rota` =
        CASE
          WHEN UPPER(TRIM(COALESCE(rar.`mercado`, ''))) LIKE '%NACION%' THEN 1
          WHEN UPPER(TRIM(COALESCE(rar.`mercado`, ''))) LIKE '%INTERNACION%' THEN 2
          ELSE fc.`tipo_rota`
        END
    )
    AND (
      UPPER(TRIM(COALESCE(fc.`cod_sigla`, ''))) = UPPER(TRIM(COALESCE(rar.`codigo_tarifario`, '')))
      OR UPPER(TRIM(COALESCE(fc.`nome_familia_companhia`, ''))) = UPPER(TRIM(COALESCE(rar.`familia_tarifaria`, '')))
      OR UPPER(TRIM(COALESCE(fc.`nome_familia_companhia_descricao`, ''))) = UPPER(TRIM(COALESCE(rar.`familia_tarifaria`, '')))
      OR UPPER(TRIM(COALESCE(fc.`nome_familia_companhia`, ''))) = UPPER(TRIM(COALESCE(rar.`familia_tarifaria_normalizada`, '')))
    )
    AND (
      UPPER(TRIM(COALESCE(ca.`iata_cia`, ''))) = UPPER(TRIM(COALESCE(rar.`companhia`, '')))
      OR UPPER(TRIM(COALESCE(ca.`nome_cia`, ''))) LIKE CONCAT('%', UPPER(TRIM(COALESCE(rar.`companhia`, ''))), '%')
      OR (UPPER(TRIM(COALESCE(rar.`companhia`, ''))) = 'AZUL' AND UPPER(TRIM(COALESCE(ca.`iata_cia`, ''))) = 'AD')
      OR (UPPER(TRIM(COALESCE(rar.`companhia`, ''))) = 'GOL' AND UPPER(TRIM(COALESCE(ca.`iata_cia`, ''))) = 'G3')
      OR (UPPER(TRIM(COALESCE(rar.`companhia`, ''))) = 'LATAM' AND UPPER(TRIM(COALESCE(ca.`iata_cia`, ''))) IN ('LA', 'JJ'))
    )
  ORDER BY
    CASE
      WHEN UPPER(TRIM(COALESCE(fc.`cod_sigla`, ''))) = UPPER(TRIM(COALESCE(rar.`codigo_tarifario`, ''))) THEN 0
      ELSE 1
    END,
    CASE
      WHEN fc.`tipo_rota` =
        CASE
          WHEN UPPER(TRIM(COALESCE(rar.`mercado`, ''))) LIKE '%NACION%' THEN 1
          WHEN UPPER(TRIM(COALESCE(rar.`mercado`, ''))) LIKE '%INTERNACION%' THEN 2
          ELSE fc.`tipo_rota`
        END THEN 0
      ELSE 1
    END,
    fc.`posicao` ASC,
    fc.`codgFamiliaCompanhia` ASC
  LIMIT 1
)
WHERE rar.`codg_familia_companhia` IS NULL;

-- Validacao rapida apos executar:
-- SELECT id, companhia, mercado, familia_tarifaria, codigo_tarifario, codg_familia_companhia
-- FROM regra_aerea_reembolso
-- ORDER BY companhia, familia_tarifaria, momento;
