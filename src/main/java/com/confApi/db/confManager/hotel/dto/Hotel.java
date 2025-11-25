package com.confApi.db.confManager.hotel.dto;

import java.io.Serializable;
import java.util.List;

import com.confApi.db.confManager.cidade.Cidade;
import com.confApi.db.confManager.hotel.model.HotelDetalhesHotel;
import com.confApi.db.confManager.hotel.model.HotelImagens;
import com.confApi.db.confManager.sistema.Sistema;
import lombok.Data;

@Data
public class Hotel implements Serializable {

    private Integer codgHotel;
    private Cidade codgCidade;
    private Sistema codgSistema;
    private String identificacaoHotelSitema;
    private String nomeHotel;
    private String urlImagemHotel;
    private String endereco1;
    private String endereco2;
    private String endereco3;
    private String telefone;
    private String fax;
    private String email;
    private Double estrela;
    private String mapa;
    private String latitude;
    private String longitude;
    private String checkin;
    private String checkout;
    private String horarioCafe;
    private List<HotelDetalhesHotel> hotelDetalhesList;
    private List<HotelImagens> hotelImagemList;
    private String descricao;
    private Boolean recomendado = false;

    public Hotel(Integer codgHotel) {
        this.codgHotel = codgHotel;
    }

    public Hotel() {
    }

    public Hotel(Integer codgHotel, Cidade codgCidade, Sistema codgSistema, String identificacaoHotelSitema,
                 String nomeHotel, String urlImagemHotel, String endereco1, String endereco2, String endereco3,
                 String telefone, String fax, String email, Double estrela, String mapa, String latitude,
                 String longitude, String descricao, Boolean recomendado) {
        this.codgHotel = codgHotel;
        this.codgCidade = codgCidade;
        this.codgSistema = codgSistema;
        this.identificacaoHotelSitema = identificacaoHotelSitema;
        this.nomeHotel = nomeHotel;
        this.urlImagemHotel = urlImagemHotel;
        this.endereco1 = endereco1;
        this.endereco2 = endereco2;
        this.endereco3 = endereco3;
        this.telefone = telefone;
        this.fax = fax;
        this.email = email;
        this.estrela = estrela;
        this.mapa = mapa;
        this.latitude = latitude;
        this.longitude = longitude;
        this.descricao = descricao;
        this.recomendado = recomendado;
    }

    public Hotel(Integer codgHotel, Cidade codgCidade, Sistema codgSistema, String identificacaoHotelSitema,
                 String nomeHotel, String urlImagemHotel, String endereco1, String endereco2, String endereco3,
                 String telefone, String fax, String email, Double estrela, String mapa, String latitude,
                 String longitude, String checkin, String checkout, String horarioCafe,String descricao,
                 Boolean recomendado) {
        this.codgHotel = codgHotel;
        this.codgCidade = codgCidade;
        this.codgSistema = codgSistema;
        this.identificacaoHotelSitema = identificacaoHotelSitema;
        this.nomeHotel = nomeHotel;
        this.urlImagemHotel = urlImagemHotel;
        this.endereco1 = endereco1;
        this.endereco2 = endereco2;
        this.endereco3 = endereco3;
        this.telefone = telefone;
        this.fax = fax;
        this.email = email;
        this.estrela = estrela;
        this.mapa = mapa;
        this.latitude = latitude;
        this.longitude = longitude;
        this.checkin = checkin;
        this.checkout = checkout;
        this.horarioCafe = horarioCafe;
        this.descricao = descricao;
        this.recomendado = recomendado;
    }

    public Hotel(Integer codgHotel, Cidade codgCidade, Sistema codgSistema, String identificacaoHotelSitema, String nomeHotel,
                 String urlImagemHotel, String endereco1, String endereco2, String endereco3, String telefone, String fax, String email,
                 Double estrela, String mapa, String latitude, String longitude, String checkin, String checkout, String horarioCafe,
                 List<HotelDetalhesHotel> hotelDetalhesList, List<HotelImagens> hotelImagemList, String descricao, Boolean recomendado) {
        this.codgHotel = codgHotel;
        this.codgCidade = codgCidade;
        this.codgSistema = codgSistema;
        this.identificacaoHotelSitema = identificacaoHotelSitema;
        this.nomeHotel = nomeHotel;
        this.urlImagemHotel = urlImagemHotel;
        this.endereco1 = endereco1;
        this.endereco2 = endereco2;
        this.endereco3 = endereco3;
        this.telefone = telefone;
        this.fax = fax;
        this.email = email;
        this.estrela = estrela;
        this.mapa = mapa;
        this.latitude = latitude;
        this.longitude = longitude;
        this.checkin = checkin;
        this.checkout = checkout;
        this.horarioCafe = horarioCafe;
        this.hotelDetalhesList = hotelDetalhesList;
        this.hotelImagemList = hotelImagemList;
        this.descricao = descricao;
        this.recomendado = recomendado;
    }

    @Override
    public String toString() {
        return "Hotel{" + "codgHotel=" + codgHotel
                + ", codgCidade=" + codgCidade
//                + ", codgSistema=" + codgSistema
                + ", identificacaoHotelSitema=" + identificacaoHotelSitema
                + ", nomeHotel=" + nomeHotel
                + ", urlImagemHotel=" + urlImagemHotel
//                + ", endereco1=" + endereco1
//                + ", endereco2=" + endereco2
//                + ", endereco3=" + endereco3
                + ", telefone=" + telefone
                + ", fax=" + fax
                + ", email=" + email
                + ", estrela=" + estrela
                + ", mapa=" + mapa
                + ", latitude=" + latitude
                + ", longitude=" + longitude
                + ", checkin=" + checkin
                + ", checkout=" + checkout
                + ", horarioCafe=" + horarioCafe
                + ", hotelDetalhesList=" + hotelDetalhesList
                + ", hotelImagemList=" + hotelImagemList
                + ", descricao=" + descricao
                + '}';
    }


}

