package com.confApi.carros.dto;

import lombok.Data;

import java.util.List;

@Data
public class EmitirCarroRequestDTO {
    private String token;
    private String localizador;
    private List<PagamentoCarroHub> pagamentoList;
    private String ip;
    private SistemaCarroHub sistema;
}
