package com.confApi.db.confManager.regraAereaAlteracao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegraAereaAlteracaoCalculoResponse {
    private String moeda;
    private BigDecimal valorTarifaBase;
    private BigDecimal valorNovaTarifa;
    private BigDecimal valorMulta;
    private BigDecimal diferencaTarifaria;
    private BigDecimal totalPrevisto;
    private BigDecimal percentualMultaAplicado;
    private String resumo;
}
