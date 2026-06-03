package com.confApi.carros.dto;

import lombok.Data;

@Data
public class FormasPagamentoParcelasCarroHub {
    private Double percentualJuros;
    private Integer idParcelamento;
    private Boolean jurosSobreParcelamento;
    private Integer numeroParcelas;
    private Double valorPrimeiraParcela;
    private Double valorDemaisParcelas;
    private Double valorTotal;
}
