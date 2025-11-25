package com.confApi.hub.hotel.dto;

import lombok.*;
import java.util.*;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class ReservasHotelRs {
    private Date dataCriacao;
    private InfoGlobal infoGlobal;
    private String identificador;
    private String status;
    private Date dataEntrada;
    private Date dataSaida;
    private List<HotelAcomodacao> acomodacoes = new ArrayList<>();
}
