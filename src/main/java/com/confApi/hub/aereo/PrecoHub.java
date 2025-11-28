package com.confApi.hub.aereo;

import lombok.Data;

@Data
public class PrecoHub {
    private Double eqCambio;
    private String eqMoeda;
    private String moeda;
    private PrecoTipoHub precoAdulto;
    private PrecoTipoHub precoBebe;
    private PrecoTipoHub precoCrianca;
    private Double tarifa;
    //    private Boolean tarifaOperadora;
//    private Boolean tarifaPrivada;
    private Double taxa;
    private Double total;
    private Double totalGeral;
    private Double totalTarifa;
    private Double totalTarifaNet;
    private Double totalTaxaAssento;
    private Double totalTaxaBagagem;
    private Double totalTaxaDeCombustivel;
    private Double totalTaxaEmbarque;
    private Double totalTaxaMenorDesacompanhado;
    private Double totalTaxaServico;
    private Double valorEMD;
    private Double valorFEE;
    private Double valorRAV;
}
