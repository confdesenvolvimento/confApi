package com.confApi.hoteis.model.reserva;

import lombok.*;

@Data
@NoArgsConstructor

@ToString
@EqualsAndHashCode
public class HotelCarregaModelFront {
    private String identificador;

    public HotelCarregaModelFront(String identificador) {
        this.identificador = identificador;
    }

}
