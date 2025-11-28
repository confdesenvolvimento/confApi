package com.confApi.endPoints.unidade;

import lombok.Data;

@Data
public class Unidade {
    private Integer codgUnidade;
    private String nomeUnidade;
    private String codgSistemaBackOffice;
    private Integer status;
    private Integer idWoobaUnidade;

    public Unidade(com.confApi.db.confManager.unidade.dto.Unidade unidade) {
        this.codgUnidade = unidade.getCodgUnidade();
        this.nomeUnidade = unidade.getNomeUnidade();
        this.codgSistemaBackOffice = unidade.getCodgSistemaBackOffice();
        this.status = unidade.getStatus();
        this.idWoobaUnidade = unidade.getIdWoobaUnidade();
    }
}
