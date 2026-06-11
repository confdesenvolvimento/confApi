package com.confApi.carros.dto;

import lombok.Data;

@Data
public class PagamentoCarroHub {
    private Integer idParcelamento;
    private String formasPagamento;
    private CartaoDeCreditoCarroHub cartao;

    public PagamentoCarroHub(String obj) {
        this.formasPagamento = obj;
    }
}
