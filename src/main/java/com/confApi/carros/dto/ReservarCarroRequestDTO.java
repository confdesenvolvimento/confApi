package com.confApi.carros.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReservarCarroRequestDTO {
    private String token;
    private Integer idLocalRetirada;
    private Integer idLocalEntrega;
    private List<AdditionalItemHub> itensAdicionais;
    private CarroBookingCondutorHub condutor;
    private String ipEmissao;
    private SistemaCarroHub sistema;
}
