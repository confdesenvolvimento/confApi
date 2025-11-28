package com.confApi.hub.aereo;

import lombok.Data;

import java.util.List;

@Data
public class FamiliaPrecoHub {
    private Boolean bagagemInclusa = false;
    private Double bagagemPeso = 0.0;
    private Integer bagagemQuantidade = 0;
    private String baseTarifaria;
    private String cabine;
    private String classe;
    private String tipo;
    private FamiliaHub familia;
    private PrecoHub preco;
    private String identificacaoDeVoo;
    private List<FamiliaPrecoInterHub> familiaPrecoInterList;
    private String identificacaoDaViagem;
}
