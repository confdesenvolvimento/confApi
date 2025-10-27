package com.confApi.db.confManager.faturas.dto.model;

import lombok.Data;

@Data
public class Carro {
    private String pax;
    private String numresv;
    private String checkin;
    private String checkout;
    private String emissao;
    private int numvend;
    private double tarifa;
    private double taxa;
    private double cambio;
    private String moeda;
    private int qtddias;
    private String tipo;
    private String formaPagamento;
    private double tarifam;
    private double fee;
    private double imposto;
    private double liquido;
    private double cartao;
    private double comissao;
    private double desconto;
    private double incentivo;
    private String cidadeRetirada;
    private String cidadeDevolucao;
    private String modeloCarro;
}
