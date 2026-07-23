package com.confApi.aereo.dto;

import lombok.Data;

@Data
public class FamiliaDetalheModel {
    private Integer posicao;
    private String descricaoDetalhe;
    private Integer isContempla;

    public FamiliaDetalheModel(Integer posicao, String descricaoDetalhe, Integer isContempla) {
        this.posicao = posicao;
        this.descricaoDetalhe = descricaoDetalhe;
        this.isContempla = isContempla;
    }
}
