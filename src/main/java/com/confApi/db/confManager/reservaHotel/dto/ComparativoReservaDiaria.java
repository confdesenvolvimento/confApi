package com.confApi.db.confManager.reservaHotel.dto;

import lombok.Data;

import java.util.Date;

@Data
public class ComparativoReservaDiaria {
    private String unidade;
    private String agencia;
    private String localizador;
    private String sistema;
    private String nomeHotel;
    private String codgHotel;
    private Integer qtdQuartos;
    private Integer qtdAdt;
    private Integer qtdChd;
    private Integer qtdInf;
    private Integer qtdDiarias;
    private Date dataEntrada;
    private Date dataSaida;
    private String status;
    //    private ComparativoReservaValores comparativoReservaDiariaValores;
    private Date prazoEmissao;
    private String hospede;

    private Double valorTotalEstadiaNetAntigo = 0.0;
    private Double mediaDiariaAntigo = 0.0;
    private Double valorTotalEstadiaNetNovo = 0.0;
    private Double mediaDiariaNovo = 0.0;
    private Double valorTotalEstadiaNetVariacao = 0.0;
    private Double mediaDiariaVariacao = 0.0;
    private Double valorTotalEstadiaNetPercentual = 0.0;
    private Double mediaDiariaPercentual = 0.0;
}