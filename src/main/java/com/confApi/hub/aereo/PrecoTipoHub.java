package com.confApi.hub.aereo;

import lombok.Data;

@Data
public class PrecoTipoHub {
    private Integer eqValorTarifa;
    private Integer eqValorTaxaServico;
    private Integer quantidade;
    private Double valorFee = 0.0;
    private Double valorRav;
    private Double valorTarifa = 0.0;
    private Double valorTarifaNet = 0.0;
    private Double valorTaxaAssento;
    private Double valorTaxaBagagem;
    private Double valorTaxaCombustivel;
    private Double valorTaxaEmbarque;
    private Double valorTaxaMenorDesacompanhado;
    private Double valorTaxaServico;
    private Double valorMKP = 0.0;
    private Integer tipoMKP = 0;
}
