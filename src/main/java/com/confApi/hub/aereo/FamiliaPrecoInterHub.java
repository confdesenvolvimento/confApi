package com.confApi.hub.aereo;

import lombok.Data;

@Data
public class FamiliaPrecoInterHub {
    private Boolean bagagemInclusa = false;
    private Double bagagemPeso = 0.0;
    private Integer bagagemQuantidade = 0;
    private String baseTarifaria;
    private String cabine;
    private String classe;
    private String tipo;
    private FamiliaHub familia;
    private PrecoHub preco;
    private String numeroVoo;
}
