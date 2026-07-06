package com.confApi.db.confManager.carro.dto;

import lombok.Data;

import java.util.List;

@Data
public class SalvarReservaCarroResponseDTO {

    private Integer codgReservaCarro;
    private Integer codgCarro;
    private List<Integer> codgCondutores;
    private List<Integer> codgItens;
}
