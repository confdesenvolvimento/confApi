package com.confApi.hoteis.model.reserva;

import java.util.Date;
import java.util.List;

public class HotelReservaFront {
    private String codgReferencia;
    private String nome;
    private String descricao;
    private String codgHotel;
    private String urlImagem;
    private List<ReservasHotelRsFront> reservasHotelRsList;
    private Date dataEntrada;
    private Date dataSaida;
}
