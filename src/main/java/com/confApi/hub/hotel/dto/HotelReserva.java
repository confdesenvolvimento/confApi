package com.confApi.hub.hotel.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serial;
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

    @Serial
    private static final long serialVersionUID = -1657836908530033767L;

    private String codgReferencia;
    private String nome;
    private String descricao;
    private String codgHotel;
    private String urlImagem;
    private List<ReservasHotelRs> reservasHotelRsList;
    private Date dataEntrada;
    private Date dataSaida;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
    private List<com.confApi.operacao.DivergenciaOperacao> divergenciasOperacao;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String observacaoOmnibees;

    public HotelReserva(String codgReferencia, String nome, String descricao, String codgHotel, String urlImagem,
                        List<ReservasHotelRs> reservasHotelRsList, Date dataEntrada, Date dataSaida) {
        this(codgReferencia, nome, descricao, codgHotel, urlImagem);
        this.reservasHotelRsList = reservasHotelRsList;
        this.dataEntrada = dataEntrada;
        this.dataSaida = dataSaida;
    }

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
