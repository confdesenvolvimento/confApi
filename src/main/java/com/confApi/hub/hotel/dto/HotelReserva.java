package com.confApi.hub.hotel.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class HotelReserva implements Serializable {

    private String codgReferencia;
    private String nome;
    private String descricao;
    private String codgHotel;
    private String urlImagem;
    private List<ReservasHotelRs> reservasHotelRsList;
    private Date dataEntrada;
    private Date dataSaida;




    public HotelReserva(String codgReferencia, String nome, String descricao, String codgHotel) {
        this.codgReferencia = codgReferencia;
        this.nome = nome;
        this.descricao = descricao;
        this.codgHotel = codgHotel;
    }
    public HotelReserva(String codgReferencia, String nome, String descricao, String codgHotel,Date dataEntrada,Date dataSaida) {
        this.codgReferencia = codgReferencia;
        this.nome = nome;
        this.descricao = descricao;
        this.codgHotel = codgHotel;
        this.dataEntrada = dataEntrada;
        this.dataSaida = dataSaida;
    }

    public HotelReserva(String codgReferencia, String nome, String descricao, String codgHotel, String urlImagem) {
        this.codgReferencia = codgReferencia;
        this.nome = nome;
        this.descricao = descricao;
        this.codgHotel = codgHotel;
        this.urlImagem = urlImagem;
    }
}
