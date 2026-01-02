package com.confApi.hub.hotel.dto;

import lombok.*;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class HotelCancelarModel {
    private String localizador;
    private Integer codgHotel;
    private Date dataEntrada;
    private Date dataSaida;
    private String motivoCancelamento;
}
