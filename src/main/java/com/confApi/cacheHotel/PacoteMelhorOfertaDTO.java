package com.confApi.cacheHotel;

import lombok.Data;

import java.util.Date;

@Data
public class PacoteMelhorOfertaDTO {
    private String origem;
    private String destino;
    private String nomeCidade;
    private String nomeEstado;
    private String nomePais;
    private Date dataIda;
    private Date dataVolta;
    private Integer quantidadeNoites;
    private Integer quantidadeAdultos;
    private Integer quantidadeQuartos;
    private String companhiaIda;
    private String companhiaVolta;
    private Double valorAereoPorPessoa;
    private Double valorAereoTotal;
    private HotelResumo hotelEconomico;
    private Double valorTotal;
    private Double valorPorPessoa;
    private Boolean hotelOutraCidade;
    private Date expiraEm;

    @Data
    public static class HotelResumo {
        private String nomeHotel;
        private Integer categoria;
        private String nomeQuarto;
        private String regime;
        private Double valorTotal;
    }
}
