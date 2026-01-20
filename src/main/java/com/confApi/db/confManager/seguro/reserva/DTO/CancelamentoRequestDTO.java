package com.confApi.db.confManager.seguro.reserva.DTO;

import lombok.Data;

@Data
public class CancelamentoRequestDTO {

    private Integer codgReserva;
    private Integer codgUsuarioCancelamento;
    private String descricaoMotivoCancelamento;
    private String localizador;
}
