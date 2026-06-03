package com.confApi.carros.dto;

import lombok.Data;

@Data
public class PagamentoCarroResponseHub {
    private String formaPagamento;
    private Object cartaoCredito;
    private Double valorEquivalente;
    private Double valor;
    private Double cambio;
    private String urlComprovanteBancario;
    private String status;
    private Boolean pagamentoConfirmado;
    private String pagamentoUrl;
}
