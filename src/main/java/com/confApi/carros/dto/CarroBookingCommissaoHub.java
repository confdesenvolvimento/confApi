package com.confApi.carros.dto;

import lombok.Data;

@Data
public class CarroBookingCommissaoHub {
    private Double percentual;
    private Double valor;
    private Boolean descontoComissaoAgente;
    private String descricao;
    private String descricaoPlanoComissao;
}
