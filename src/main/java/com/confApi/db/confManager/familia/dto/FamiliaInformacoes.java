package com.confApi.db.confManager.familia.dto;

import lombok.Data;

import java.io.Serializable;
@Data
public class FamiliaInformacoes implements Serializable {

    private Integer codgFamiliaInformacoes;
    private String descricao;
    private Integer flagContempla;
    private Integer posicao;
}
