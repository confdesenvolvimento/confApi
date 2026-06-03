package com.confApi.carros.dto;

import lombok.Data;

@Data
public class CarroBookingItemHub {
    private String code;
    private String descricao;
    private Double valor;
    private Double valorEquivalente;
    private String tipo;
    private Integer quantidade;
}
