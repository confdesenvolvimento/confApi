package com.confApi.hoteis.model.reserva;

import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class ReservasHotelRsFront {
    private Date dataCriacao;
    private InfoGlobalFront infoGlobal;
    private String identificador;
    private String status;
    private Date dataEntrada;
    private Date dataSaida;
    private List<HotelAcomodacaoFront> acomodacoes = new ArrayList<>();
}
