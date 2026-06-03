package com.confApi.carros.dto;

import lombok.Data;

@Data
public class FormasPagamentoCarroRequestDTO {
    private String token;
    private String localizador;
    private SistemaCarroHub sistema;
}
