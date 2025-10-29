package com.confApi.db.wooba.checkin.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class Checkin implements Serializable {

    private String id;
    private String trecho;
    private String nomeAgencia;
    private String nomeUnidade;
    private String numeroDoBilhete;
    private String numeroBilheteOriginal;
    private String tktBilhete;
    private String logomarca;
    private String localizadorCompanhia;
    private Date data;
    private String companhia;
    private String nomeCompleto;
    private String email;
    private List<PassageiroDto> passageiros;
    private Integer reemissao;
    private Integer statusReserva;
    private String nomeCia;
    private List<TrechoCheckin> trechosIda;
    private List<TrechoCheckin> trechosVolta;
    private List<TrechoCheckin> trechosMultiplaConexao;
}
