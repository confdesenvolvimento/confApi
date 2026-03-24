package com.confApi.cacheHotel.hotel.DTO;

import lombok.Data;

import java.util.Date;

@Data
public class CacheHotelDTO {

    private String codgCidade;
    private String nomeCidade;
    private String nomePais;
    private String nomeEstado;
    private String nomeHotel;
    private Integer categoria;
    private String endereco;
    private String nomeQuarto;
    private String regime;
    private Date checkin;
    private Date checkout;
    private Double totalDiarias;
    private Double taxas;
    private Double diariaMedia;
    private Integer qtdHospedes;
    private Date dataPesquisa;
    private Date dataExpiracao;
    private String latitude;
    private String longitude;
    private String nomeSistema;
}
