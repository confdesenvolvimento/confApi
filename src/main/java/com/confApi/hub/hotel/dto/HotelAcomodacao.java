package com.confApi.hub.hotel.dto;

import lombok.*;


import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class HotelAcomodacao {
    private String codgPlanoTarifa;
    private String codgRoom;
    private String siglaTipoQuarto;
    private String nomeQuarto;
    private String nomeQuartoExtenso;
    private Boolean isSelecionado = false;
    private String nomeTipoQuarto;
    private String descricaoTipoCama;
    private String regime;
    private String descricaoOferta;
    private String descricaoTipoDeTarifa;
    private String descricaoTipoAcomodacao;
    private TarifaHotel tarifaHotel;
    private Integer vagasDisponiveis = 0;
    private Boolean isPrePagamento = false;
    private Boolean isNaoReembolsavel = false;
    private String formaPagamento;
    private Integer maximoHospedes = 0;
    private Integer minimoHospedes = 0;
    private Integer maximoCriancas = 0;
    private Integer minimoCriancas = 0;
    private List<Hospedes> hospedes;
    private List<HotelTaxasPoliticas> taxasPoliticas = new ArrayList<>();
    private List<HotelPoliticaCancelamento> politicaCancelamento = new ArrayList<>();

    private String sistema;
}
