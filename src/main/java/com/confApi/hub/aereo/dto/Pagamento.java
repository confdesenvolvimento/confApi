package com.confApi.hub.aereo.dto;

import java.util.Date;

public class Pagamento {
    private CartaoDeCredito cartaoDeCredito;
    private Date data;
    private Integer fEE;
    private String moeda;
    private Integer multaReemissao;
    private Integer oBFEE;
    private Integer rAV;
    private Double tarifa;
    private Double taxa;
    private Integer taxaAssento;
    private Integer taxaBagagem;
    private Double taxaRepasseTerceiros;
    private Integer tipo;
    private Integer formaDePagamento;


    public Pagamento() {
    }

    public Pagamento(CartaoDeCredito cartaoDeCredito, Date data, Integer fEE,
                     String moeda, Integer multaReemissao, Integer oBFEE,
                     Integer rAV, Double tarifa, Double taxa, Integer taxaAssento,
                     Integer taxaBagagem, Double taxaRepasseTerceiros, Integer tipo, Integer formaDePagamento) {
        this.cartaoDeCredito = cartaoDeCredito;
        this.data = data;
        this.fEE = fEE;
        this.moeda = moeda;
        this.multaReemissao = multaReemissao;
        this.oBFEE = oBFEE;
        this.rAV = rAV;
        this.tarifa = tarifa;
        this.taxa = taxa;
        this.taxaAssento = taxaAssento;
        this.taxaBagagem = taxaBagagem;
        this.taxaRepasseTerceiros = taxaRepasseTerceiros;
        this.tipo = tipo;
        this.formaDePagamento = formaDePagamento;
    }

    public CartaoDeCredito getCartaoDeCredito() {
        return cartaoDeCredito;
    }

    public void setCartaoDeCredito(CartaoDeCredito cartaoDeCredito) {
        this.cartaoDeCredito = cartaoDeCredito;
    }

    public Date getData() {
        return data;
    }

    public void setData(Date data) {
        this.data = data;
    }

    public Integer getfEE() {
        return fEE;
    }

    public void setfEE(Integer fEE) {
        this.fEE = fEE;
    }

    public String getMoeda() {
        return moeda;
    }

    public void setMoeda(String moeda) {
        this.moeda = moeda;
    }

    public Integer getMultaReemissao() {
        return multaReemissao;
    }

    public void setMultaReemissao(Integer multaReemissao) {
        this.multaReemissao = multaReemissao;
    }

    public Integer getoBFEE() {
        return oBFEE;
    }

    public void setoBFEE(Integer oBFEE) {
        this.oBFEE = oBFEE;
    }

    public Integer getrAV() {
        return rAV;
    }

    public void setrAV(Integer rAV) {
        this.rAV = rAV;
    }

    public Double getTarifa() {
        return tarifa;
    }

    public void setTarifa(Double tarifa) {
        this.tarifa = tarifa;
    }

    public Double getTaxa() {
        return taxa;
    }

    public void setTaxa(Double taxa) {
        this.taxa = taxa;
    }

    public Integer getTaxaAssento() {
        return taxaAssento;
    }

    public void setTaxaAssento(Integer taxaAssento) {
        this.taxaAssento = taxaAssento;
    }

    public Integer getTaxaBagagem() {
        return taxaBagagem;
    }

    public void setTaxaBagagem(Integer taxaBagagem) {
        this.taxaBagagem = taxaBagagem;
    }

    public Double getTaxaRepasseTerceiros() {
        return taxaRepasseTerceiros;
    }

    public void setTaxaRepasseTerceiros(Double taxaRepasseTerceiros) {
        this.taxaRepasseTerceiros = taxaRepasseTerceiros;
    }

    public Integer getTipo() {
        return tipo;
    }

    public void setTipo(Integer tipo) {
        this.tipo = tipo;
    }

    public Integer getFormaDePagamento() {
        return formaDePagamento;
    }

    public void setFormaDePagamento(Integer formaDePagamento) {
        this.formaDePagamento = formaDePagamento;
    }

    @Override
    public String toString() {
        return "Pagamento{" +
                "cartaoDeCredito=" + cartaoDeCredito +
                ", data=" + data +
                ", fEE=" + fEE +
                ", moeda='" + moeda + '\'' +
                ", multaReemissao=" + multaReemissao +
                ", oBFEE=" + oBFEE +
                ", rAV=" + rAV +
                ", tarifa=" + tarifa +
                ", taxa=" + taxa +
                ", taxaAssento=" + taxaAssento +
                ", taxaBagagem=" + taxaBagagem +
                ", taxaRepasseTerceiros=" + taxaRepasseTerceiros +
                ", tipo=" + tipo +
                ", formaDePagamento=" + formaDePagamento +
                '}';
    }
}

