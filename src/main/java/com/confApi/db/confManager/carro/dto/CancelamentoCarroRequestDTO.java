package com.confApi.db.confManager.carro.dto;

import lombok.Data;

@Data
public class CancelamentoCarroRequestDTO {

    private Integer codgReserva;
    private Integer codgUsuarioCancelamento;
    private String descricaoMotivoCancelamento;
    private String localizador;
}
