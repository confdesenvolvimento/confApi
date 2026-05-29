package com.confApi.carros.dto;

import lombok.Data;

@Data
public class ValoresCarro {
    private String moeda;
    private Double cambioUDS;
    private Double valorNet;
    private Double valorNetEquivalente;
    private Double valorTarifa;
    private Double valorTarifaEquivalente;
    private Double valorTotal;
    private Double valorTotalEquivalente;
    private Double taxaPrepaga;
    private Double tacaPrepagaEquivalente;
    private Double taxaLocal;
    private Double taxaLocalEquivalente;
    private Double diariaEstimada;
    private Double diariaEstimadaEquivalente;
    private Double diaria;
    private Double diariaEquivalente;
    private Double percentualDescontoAplicado;
    private Double desconto;
    private Double descontoEquivalente;
    private Double credito;
    private Double creditoEquivalente;
    private Double cambioCalculado;
    private Double percentualRAV;
    private Double rav;
    private Double ravEquivalente;
    private Double percentualTaxaEmissao;
    private Double valorTaxaEmissao;
    private Double valorTaxaEmissaoEquivalente;
    private Double taxas;
    private Double taxasEquivalente;
    private Double totalPrepago;
    private Double totalPrepagoEquivalente;
    private Double totalPOD;
    private Double totalPODEquivalente;
    private Double valorReembolso;
    private Double valorReembolsoEquivalente;
    private Boolean taxaPrepagoAdicionadoTarifa;
    private Double percentualMaximoDesconto;
    private Double valorTotalTaxaEmissao;
    private Double valorTotalTaxaEmissaoEquivalente;
    private Double valorTotalComTaxaEmissao;
    private Double valorTotalComTaxaEmissaoEquivalente;
    private Double valorTotalEstrangeiro;
    private Object moedaEstrangeira;
    private Object descricaoEstrangeira;
}
