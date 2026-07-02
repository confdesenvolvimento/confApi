package com.confApi.db.clube.campanha;

import lombok.Data;

import java.io.Serializable;

@Data
public class RankingEntry implements Serializable {
    private Integer position;
    private String codgUsuario;
    private String codgAgencia;
    private String nomeAgencia;
    private Integer totalVendas=0;
    private Integer totalBilhetes=0;
    private Integer totalValorCount=0;
    private double totalValor=0.0;
    private Boolean isUserLogged;
    private String nomeUnidade;
    private Integer codgUnidade;
    private String nomeUsuario;
    private String nomeExibicaoRanking;
    private Integer contador=0;
}
