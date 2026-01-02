package com.confApi.hub.hotel.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class HotelPesquisaModel {
    private HotelDestino destinoHotel;
    private Date dataEntrada;
    private Date dataSaida;
    private Integer quantidadeNoites;
    private Integer quantidadeQuartos = 1;
    private String codgCidade;
    private String nomeCidade;
    private String codgEstado;
    private String nomeEstado;
    private String codgPais;
    private String nomePais;
    private Integer codgCidadeDb;
    private List<HotelPesquisaQuarto> quartoPesquisa = new ArrayList<HotelPesquisaQuarto>();
}
