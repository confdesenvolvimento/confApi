package com.confApi.hub.hotel.dto;

import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class HotelTaxasPoliticas {
    private String codgReferencia;
    private String nome;
    private String descricao;
    private Double valor=0.0;


}

