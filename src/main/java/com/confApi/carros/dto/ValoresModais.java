package com.confApi.carros.dto;

import lombok.Data;

@Data
public class ValoresModais {
    private Integer quantidadeDiaria;
    private Double troca;
    private Double tarifa;
    private Double tarifaEquivalente;
    private Double tarifaNet;
    private Double tarifaNetEquivalente;
    private Double taxaPrepaga;
    private Double taxaPrepagaEquivalente;
    private Double taxaLocal;
    private Double taxaLocalEquivalente;
    private Double percentualTaxa;
    private Double taxas;
    private Double taxasEquivalentes;
    private Double percentualRAV;
    private Double rav;
    private Double ravEquivalente;
    private Double percentualTaxaEmissao;
    private Double taxaEmissao;
    private Double taxaEmissaoEquivalente;
    private Double totalReserva;
    private Double totalReservaEquivalente;
    private Double percentualComissao;
    private Double comissao;
    private Double comissaoEquivalente;
}
