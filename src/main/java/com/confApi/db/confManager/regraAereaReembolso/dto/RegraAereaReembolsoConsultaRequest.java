package com.confApi.db.confManager.regraAereaReembolso.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RegraAereaReembolsoConsultaRequest {
    private String companhia;
    private Integer codgFamiliaCompanhia;
    private String mercado;
    private String cabine;
    private String familiaTarifaria;
    private String familiaTarifariaNormalizada;
    private String codigoTarifario;
    private String classeReserva;
    private String momento;
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
