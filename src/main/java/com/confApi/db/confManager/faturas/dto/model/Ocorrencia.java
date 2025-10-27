package com.confApi.db.confManager.faturas.dto.model;

import lombok.Data;

import java.util.Date;
@Data
public class Ocorrencia {
    private int numero;
    private double valor;
    private int numdoc;
    private String tipodoc;
    private String obs;
    private int numvend;
    private Date data;
    private String numtkt;
    private String codcia;
    private String loc;
    private String pax;
    private String tipo;

}
