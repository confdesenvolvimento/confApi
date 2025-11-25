package com.confApi.db.confManager.reservaValor;

import com.confApi.db.confManager.passageiro.Passageiro;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReservaValor implements Serializable {
    @JsonIgnore
    private int codgReservaValor;
    private Passageiro codgPassageiro;
    private Double valorTarifa;
    private Double valorTarifaNet;
    private Double valorTaxaEmbarque;
    private Double valorDu;
    private Double valorRc;
    private Double valorRav;
    private Double valorMkp;
    private Double percMkp;
    private Double valorTaxaCombustivel;
    private String moeda;
    private Double valorAssento;

    public String getMoeda() {
        return moeda;
    }

    public void setMoeda(String moeda) {
        this.moeda = moeda;
    }




    public int getCodgReservaValor() {
        return codgReservaValor;
    }

    public void setCodgReservaValor(int codgReservaValor) {
        this.codgReservaValor = codgReservaValor;
    }

    public Passageiro getCodgPassageiro() {
        return codgPassageiro;
    }

    public void setCodgPassageiro(Passageiro codgPassageiro) {
        this.codgPassageiro = codgPassageiro;
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

    public Double getValorDu() {
        return valorDu;
    }

    public void setValorDu(Double valorDu) {
        this.valorDu = valorDu;
    }

    public Double getValorRc() {
        return valorRc;
    }

    public void setValorRc(Double valorRc) {
        this.valorRc = valorRc;
    }

    public Double getValorRav() {
        return valorRav;
    }

    public void setValorRav(Double valorRav) {
        this.valorRav = valorRav;
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

    public Double getValorTaxaCombustivel() {
        return valorTaxaCombustivel;
    }

    public void setValorTaxaCombustivel(Double valorTaxaCombustivel) {
        this.valorTaxaCombustivel = valorTaxaCombustivel;
    }

    public Double getValorAssento() {
        return valorAssento;
    }

    public void setValorAssento(Double valorAssento) {
        this.valorAssento = valorAssento;
    }



}

