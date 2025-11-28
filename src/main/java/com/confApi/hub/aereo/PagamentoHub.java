package com.confApi.hub.aereo;

import lombok.Data;

import java.util.Date;

@Data
public class PagamentoHub {
    private CartaoDeCreditoHub cartaoDeCredito;
    private Date data;
    private Integer fEE;
    private String moeda;
    private Integer multaReemissao;
    private Integer oBFEE;
    private Integer rAV;
    private Double tarifa;
    private Double taxa;
    private Integer taxaAssento;
    private Integer taxaBagagem;
    private Double taxaRepasseTerceiros;
    private Integer tipo;
    private Integer formaDePagamento;
}
