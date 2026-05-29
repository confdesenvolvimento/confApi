package com.confApi.carros.dto;

import lombok.Data;

import java.util.List;

@Data
public class SelecionarCarroRequestDTO {
    private String index;
    private String supplier;
    private String token;
    private List<SistemaCarroHub> sistemas;
}
