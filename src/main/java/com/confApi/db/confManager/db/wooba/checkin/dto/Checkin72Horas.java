package com.confApi.db.confManager.db.wooba.checkin.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class Checkin72Horas implements Serializable {

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
    private String passageiro;
    private Integer reemissao;
    private Integer statusReserva;
    private String linkCheckin;
    private List<TrechoCheckin> trechosIda;
    private List<TrechoCheckin> trechosVolta;
    private List<TrechoCheckin> trechosMultiplaConexao;
    private String nomeCia;
}
