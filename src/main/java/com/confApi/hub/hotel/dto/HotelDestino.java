package com.confApi.hub.hotel.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class HotelDestino {
    private String nomeHotel;
    private Boolean isHotel = false;
    private String descricao ="";
    private String codgHotel;
}