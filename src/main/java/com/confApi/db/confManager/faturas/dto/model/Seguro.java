package com.confApi.db.confManager.faturas.dto.model;

import lombok.Data;

@Data
public class Seguro {

    private String pax;
    private String numresv;
    private String emissao;
    private int numvend;
    private double tarifa;
    private double taxa;
    private double cambio;
    private String moeda;
    private String tipo;
    private Object tipodoc;
    private String formaPagamento;
    private double tarifam;
    private double fee;
    private double imposto;
    private double liquido;
    private double cartao;
    private double comissao;
    private double desconto;
    private double incentivo;
    private Object numeroTicket;
    private String loc;
}
