package com.confApi.carros.dto;

import lombok.Data;

@Data
public class TaxasItens {
    private String descricao;
    private Double valor;
    private Double valorEquivalente;
    private Double porcentagem;
}
