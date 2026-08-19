package com.confApi.db.confManager.regraAereaAlteracao.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class RegraAereaAlteracaoConsultaRequest {
    private String companhia;
    private Integer codgCompanhiaAerea;
    private Integer codgFamiliaCompanhia;
    private String mercado;
    private String familiaTarifaria;
    private String familiaTarifariaNormalizada;
    private String codigoTarifario;
    private String classeReserva;
    private String tipoEvento;
    private String momento;

    private BigDecimal valorTarifa;
    private BigDecimal valorNovaTarifa;
    private BigDecimal valorTotalReserva;
    private BigDecimal valorTaxas;
    private BigDecimal valorNovasTaxas;
    private BigDecimal taxaServico;
    private Date dataEmissaoReserva;

    private Integer quantidadePassageiros;
    private Integer quantidadeTrechos;
    private Boolean familiaTarifariaAlterada;

    private Boolean exigirRegraAprovada;
    private Integer validadeMaximaDias;
}
