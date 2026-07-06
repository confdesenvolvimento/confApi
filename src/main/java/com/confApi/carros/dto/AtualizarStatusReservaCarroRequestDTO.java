package com.confApi.carros.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtualizarStatusReservaCarroRequestDTO {

    private Integer codgReservaCarro;

    private Integer statusReserva;
    private Integer statusPagamentoFornecedor;
    private Integer statusPagamentoCliente;

    private String bookingStatusFornecedor;
    private String pagamentoStatusFornecedor;
}
