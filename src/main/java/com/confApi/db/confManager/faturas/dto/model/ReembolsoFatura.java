package com.confApi.db.confManager.faturas.dto.model;

import lombok.Data;

import java.io.Serializable;
@Data
public class ReembolsoFatura implements Serializable {

    private static final long serialVersionUID = 1L;

    private double cartao;
    private String codcia;
    private double comissao;
    private String dataEmissao;
    private double desconto;
    private double incentivo;
    private double liquido;
    private String moeda;
    private double multa;
    private String numtkt;
    private String numdoc;
    private int numreemb;
    private String obs;
    private String passageiro;
    private double tarifa;
    private double taxa;
    private String tipodoc;
    private String loc;
    private int venda;
    private String tipo;

    public ReembolsoFatura() {
    }

    public ReembolsoFatura(double cartao, String codcia, double comissao, String dataEmissao, double desconto, double incentivo, double liquido, String moeda, double multa, String numtkt, String numdoc, int numreemb, String obs, String passageiro, double tarifa, double taxa, String tipodoc, String loc, int venda, String tipo) {
        this.cartao = cartao;
        this.codcia = codcia;
        this.comissao = comissao;
        this.dataEmissao = dataEmissao;
        this.desconto = desconto;
        this.incentivo = incentivo;
        this.liquido = liquido;
        this.moeda = moeda;
        this.multa = multa;
        this.numtkt = numtkt;
        this.numdoc = numdoc;
        this.numreemb = numreemb;
        this.obs = obs;
        this.passageiro = passageiro;
        this.tarifa = tarifa;
        this.taxa = taxa;
        this.tipodoc = tipodoc;
        this.loc = loc;
        this.venda = venda;
        this.tipo = tipo;
    }

}
