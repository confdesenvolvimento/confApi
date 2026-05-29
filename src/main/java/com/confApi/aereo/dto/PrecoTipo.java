package com.confApi.aereo.dto;

import lombok.Data;

@Data
public class PrecoTipo {

    private Integer eqValorTarifa;
    private Integer eqValorTaxaServico;
    private Integer quantidade;
    private Double valorFee = 0.0;
    private Double valorRav = 0.0;
    private Double valorTarifa = 0.0;
    private Double valorTarifaNet = 0.0;
    private Double valorTaxaAssento = 0.0;
    private Double valorTaxaBagagem = 0.0;
    private Double valorTaxaCombustivel = 0.0;
    private Double valorTaxaEmbarque = 0.0;
    private Double valorTaxaMenorDesacompanhado = 0.0;
    private Double valorTaxaServico = 0.0;
    private Double valorMKP = 0.0;
    private Double percMKP = 0.0;
    private Integer tipoMKP = 0;
    private Double totalPrecoTipo = 0.0;
}
