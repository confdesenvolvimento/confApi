package com.confApi.aereo.dto;

import lombok.Data;

import java.io.Serializable;
@Data
public class Preco implements Serializable {

    private Integer eqCambio;
    private String eqMoeda;
    private String moeda;
    private PrecoTipo precoAdulto;
    private PrecoTipo precoBebe;
    private PrecoTipo precoCrianca;
    private Double tarifa;
    private Boolean tarifaOperadora;
    private Boolean tarifaPrivada;
    private Double taxa;
    private Double total;
    private Double totalGeral = 0.0;
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
