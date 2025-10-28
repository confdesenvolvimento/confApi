package com.confApi.hub.aereo;

import java.io.Serializable;

public class ReservaValoresAereo implements Serializable {

    private Integer tipoTarifaPax;
    private Double valorTarifa = 0.0;
    private Double valorTarifaNet = 0.0;
    private Double valorTaxaEmbarque = 0.0;
    private Double taxaDu = 0.0;
    private Double taxaRc = 0.0;
    private Double taxaRav = 0.0;
    private Double valorMkp = 0.0;
    private Double percMkp = 0.0;
    private Double valorTxCombustivel = 0.0;
    private Double taxaAssento = 0.0;
    private Double totalGeral = 0.0;
    private String moeda = "BRL";

    public String getMoeda() {
        return moeda;
    }

    public void setMoeda(String moeda) {
        this.moeda = moeda;
    }



    public ReservaValoresAereo(Integer tipoTarifaPax) {
        this.tipoTarifaPax = tipoTarifaPax;
    }

    public ReservaValoresAereo() {
    }

    public Double getTotalGeral() {
        return totalGeral;
    }

    public void setTotalGeral(Double totalGeral) {
        this.totalGeral = totalGeral;
    }

    public Double getTaxaRav() {
        return taxaRav;
    }

    public void setTaxaRav(Double taxaRav) {
        this.taxaRav = taxaRav;
    }

    public Integer getTipoTarifaPax() {
        return tipoTarifaPax;
    }

    public void setTipoTarifaPax(Integer tipoTarifaPax) {
        this.tipoTarifaPax = tipoTarifaPax;
    }

    public Double getValorTarifa() {
        return valorTarifa;
    }

    public void setValorTarifa(Double valorTarifa) {
        this.valorTarifa = valorTarifa;
    }

    public Double getValorTarifaNet() {
        return valorTarifaNet;
    }

    public void setValorTarifaNet(Double valorTarifaNet) {
        this.valorTarifaNet = valorTarifaNet;
    }

    public Double getValorTaxaEmbarque() {
        return valorTaxaEmbarque;
    }

    public void setValorTaxaEmbarque(Double valorTaxaEmbarque) {
        this.valorTaxaEmbarque = valorTaxaEmbarque;
    }

    public Double getTaxaDu() {
        return taxaDu;
    }

    public void setTaxaDu(Double taxaDu) {
        this.taxaDu = taxaDu;
    }

    public Double getTaxaRc() {
        return taxaRc;
    }

    public void setTaxaRc(Double taxaRc) {
        this.taxaRc = taxaRc;
    }

    public Double getValorMkp() {
        return valorMkp;
    }

    public void setValorMkp(Double valorMkp) {
        this.valorMkp = valorMkp;
    }

    public Double getPercMkp() {
        return percMkp;
    }

    public void setPercMkp(Double percMkp) {
        this.percMkp = percMkp;
    }

    public Double getValorTxCombustivel() {
        return valorTxCombustivel;
    }

    public void setValorTxCombustivel(Double valorTxCombustivel) {
        this.valorTxCombustivel = valorTxCombustivel;
    }

    public Double getTaxaAssento() {
        return taxaAssento;
    }

    public void setTaxaAssento(Double taxaAssento) {
        this.taxaAssento = taxaAssento;
    }



}

