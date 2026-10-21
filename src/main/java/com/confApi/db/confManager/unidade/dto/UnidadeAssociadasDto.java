package com.confApi.db.confManager.unidade.dto;

import lombok.Data;

@Data
public class UnidadeAssociadasDto {
    private Integer codgUnidade;
    private String nomeUnidade;
    private String codgSistemaBackOffice;
    private Integer status;
    private Integer idWoobaUnidade;
    private boolean principal;
}
