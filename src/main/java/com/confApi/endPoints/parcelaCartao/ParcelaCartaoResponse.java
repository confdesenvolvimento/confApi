package com.confApi.endPoints.parcelaCartao;

import lombok.Data;

@Data
public class ParcelaCartaoResponse {
    private Integer numeroDaParcela;
    private Double valorPrimeiraParcela = 0.0;
    private Double valorDemaisParcelas = 0.0;
    private Double valorJuros =0.0;
    private String descricaoParcela;
}
