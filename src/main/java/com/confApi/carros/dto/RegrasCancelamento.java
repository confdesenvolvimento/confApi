package com.confApi.carros.dto;

import lombok.Data;

@Data
public class RegrasCancelamento {
    private Double valorCancelamento;
    private Integer limiteCancelamento;
    private Object periodoValidade;
    private Boolean isValorPercentual;
    private Boolean isDiasUteis;
    private String descricaoLocal;
    private Object milhasExtras;
    private Object seguros;
}
