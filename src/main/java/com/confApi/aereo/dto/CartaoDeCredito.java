package com.confApi.aereo.dto;

import lombok.Data;

@Data
public class CartaoDeCredito {
    private Integer id;
    private String autorizacao;
    private String bandeira;
    private String codigoDeSeguranca;
    private Boolean ignorarValidacao;
    private String numero;
    private String titularCPF;
    private String titularNome;
    private String validade;
    private Integer parcelas;
    private Integer financiamentoPagamento;

}
