package com.confApi.aereo.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class FamiliaPrecoInter implements Serializable {

    private Boolean bagagemInclusa;
    private Double bagagemPeso;
    private Integer bagagemQuantidade;
    private String baseTarifaria;
    private String cabine;
    private String classe;
    private String tipo;
    private Familia familia;
    private Preco preco;
    private String numeroVoo;

    public FamiliaPrecoInter() {
    }

    public FamiliaPrecoInter(Boolean bagagemInclusa, Double bagagemPeso,
                             Integer bagagemQuantidade, String baseTarifaria, String cabine,
                             String classe, String tipo, Familia familia, Preco preco, String numeroVoo) {
        this.bagagemInclusa = bagagemInclusa;
        this.bagagemPeso = bagagemPeso;
        this.bagagemQuantidade = bagagemQuantidade;
        this.baseTarifaria = baseTarifaria;
        this.cabine = cabine;
        this.classe = classe;
        this.tipo = tipo;
        this.familia = familia;
        this.preco = preco;
        this.numeroVoo = numeroVoo;
    }
}
