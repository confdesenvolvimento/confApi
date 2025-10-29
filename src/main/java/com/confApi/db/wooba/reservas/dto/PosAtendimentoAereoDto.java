package com.confApi.db.wooba.reservas.dto;

import lombok.Data;

import java.io.Serializable;
@Data
public class  PosAtendimentoAereoDto implements Serializable {

    private String sistema;
    private String unidade;
    private String agencia;
    private String localizador;
    private String companhia;
    private String trecho;
    private String primeiroPassageiro;
    private Integer status;
    private String feitaEm;
    private String limiteParaEmissao;
    private Double tarifa;
    private Double taxas;
    private String familia;
    private String baseTarifaria;
    private String classe;
    private String de;
    private String para;
    private String infoBagagem;

}
