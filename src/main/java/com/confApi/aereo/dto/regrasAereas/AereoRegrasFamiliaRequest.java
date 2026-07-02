package com.confApi.aereo.dto.regrasAereas;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AereoRegrasFamiliaRequest {
    private String companhia;
    private String nomeCompanhia;
    private Integer codgFamiliaCompanhia;
    private String mercado;
    private String origemIata;
    private String destinoIata;
    private String cabine;
    private String familiaTarifaria;
    private String codigoTarifario;
    private String classeReserva;
    private String sistemaOrigem;

    private BigDecimal valorTarifa;
    private BigDecimal valorTaxaEmbarque;
    private BigDecimal valorTaxaDu;
    private BigDecimal valorRav;
    private BigDecimal valorRc;
    private BigDecimal valorTaxaAssento;
    private BigDecimal valorTaxaBagagem;
    private BigDecimal valorOutrasTaxas;

    private Integer quantidadePassageiros;
    private Integer quantidadeTrechos;
}
