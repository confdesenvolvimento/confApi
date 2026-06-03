package com.confApi.carros.dto;

import lombok.Data;

@Data
public class CancelarReservaCarroRequestDTO {
    private String token;
    private String localizador;
    private String ip;
    private SistemaCarroHub sistema;
}
