package com.confApi.endPoints.reservaValoresAereos;

import lombok.Data;

@Data
public class ReservaValoresAereoResponse {
    private Integer tipoTarifaPax;
    private Double valorTarifa = 0.0;
    private Double valorTarifaNet = 0.0;
    private Double valorTaxaEmbarque = 0.0;
    private Double taxaDu = 0.0;
    private Double taxaRc = 0.0;
    private Double taxaRav = 0.0;
    private Double valorMkp = 0.0;
    private Double percMkp = 0.0;
    private Double valorTxCombustivel = 0.0;
    private Double taxaAssento = 0.0;
    private Double totalGeral = 0.0;
    private String moeda = "BRL";
}
