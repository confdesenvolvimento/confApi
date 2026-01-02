package com.confApi.hoteis.model.reserva;

import lombok.Data;

@Data
public class CancelarReservaRequestHotelFront {
    private String sistema;
    private String localizador;
    private Integer codgHotel;
    private String dataEntrada;
    private String dataSaida;
    private String motivo;
}
