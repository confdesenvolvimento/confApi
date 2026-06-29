package com.confApi.db.confManager.regraAereaReembolso.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class RegraAereaReembolsoCalculoResponse {
    private Boolean calculado = false;
    private String moeda;
    private BigDecimal valorTarifaConsiderada = BigDecimal.ZERO;
    private BigDecimal valorTaxasConsideradas = BigDecimal.ZERO;
    private BigDecimal valorBaseMulta = BigDecimal.ZERO;
    private BigDecimal valorMulta = BigDecimal.ZERO;
    private BigDecimal valorBrutoReembolso = BigDecimal.ZERO;
    private BigDecimal valorPrevistoReembolso = BigDecimal.ZERO;
    private BigDecimal percentualReembolsoAplicado;
    private String mensagem;
    private List<String> alertas = new ArrayList<>();
}
