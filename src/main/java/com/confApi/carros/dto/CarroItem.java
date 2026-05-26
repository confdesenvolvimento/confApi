package com.confApi.carros.dto;

import lombok.Data;

@Data
public class CarroItem {
    private Integer id;
    private String code;
    private String descricao;
    private Double valor;
    private Double valorEquivalente;
    private String tipo;
}
