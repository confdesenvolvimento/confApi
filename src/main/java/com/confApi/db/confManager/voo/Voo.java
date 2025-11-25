package com.confApi.db.confManager.voo;

import com.confApi.db.confManager.aeroporto.Aeroporto;
import com.confApi.db.confManager.companhiaAerea.CompanhiaAerea;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.sql.Timestamp;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Voo implements Serializable {

    @JsonIgnore
    private int codgVoo;
    @JsonIgnore
    private int trechoCodgTrecho;

    private CompanhiaAerea codgCompanhiaAerea;

    private String numeroVoo;

    private Aeroporto codgAeroportoOrigem;

    private Aeroporto codgAeroportoDestino;

    private String baseTarifa;

    private String classeTarifa;

    private String aeronave;

    private int qtdEscalas;

    private String familia;

    private String codgFamilia;

    private Timestamp dataHoraPartida;

    private Timestamp dataHoraChegada;

    private int tipoRota;

    private int qtdBagagem;

    private double pesoBagagem;

    private CompanhiaAerea codgCompanhiaAereaOperada;

    private int flagCodeShare;

    private CompanhiaAerea codgCompanhiaAereaCodeShare;

    private String statusVoo;

    private String localizadorCia;

    private String cabine;

    public int getCodgVoo() {
        return codgVoo;
    }

    public void setCodgVoo(int codgVoo) {
        this.codgVoo = codgVoo;
    }

    public int getTrechoCodgTrecho() {
        return trechoCodgTrecho;
    }

    public void setTrechoCodgTrecho(int trechoCodgTrecho) {
        this.trechoCodgTrecho = trechoCodgTrecho;
    }

    public String getNumeroVoo() {
        return numeroVoo;
    }

    public void setNumeroVoo(String numeroVoo) {
        this.numeroVoo = numeroVoo;
    }

    public CompanhiaAerea getCodgCompanhiaAerea() {
        return codgCompanhiaAerea;
    }

    public void setCodgCompanhiaAerea(CompanhiaAerea codgCompanhiaAerea) {
        this.codgCompanhiaAerea = codgCompanhiaAerea;
    }

    public Aeroporto getCodgAeroportoOrigem() {
        return codgAeroportoOrigem;
    }

    public void setCodgAeroportoOrigem(Aeroporto codgAeroportoOrigem) {
        this.codgAeroportoOrigem = codgAeroportoOrigem;
    }

    public Aeroporto getCodgAeroportoDestino() {
        return codgAeroportoDestino;
    }

    public void setCodgAeroportoDestino(Aeroporto codgAeroportoDestino) {
        this.codgAeroportoDestino = codgAeroportoDestino;
    }

    public String getBaseTarifa() {
        return baseTarifa;
    }

    public void setBaseTarifa(String baseTarifa) {
        this.baseTarifa = baseTarifa;
    }

    public String getClasseTarifa() {
        return classeTarifa;
    }

    public void setClasseTarifa(String classeTarifa) {
        this.classeTarifa = classeTarifa;
    }

    public String getAeronave() {
        return aeronave;
    }

    public void setAeronave(String aeronave) {
        this.aeronave = aeronave;
    }

    public int getQtdEscalas() {
        return qtdEscalas;
    }

    public void setQtdEscalas(int qtdEscalas) {
        this.qtdEscalas = qtdEscalas;
    }

    public String getFamilia() {
        return familia;
    }

    public void setFamilia(String familia) {
        this.familia = familia;
    }

    public String getCodgFamilia() {
        return codgFamilia;
    }

    public void setCodgFamilia(String codgFamilia) {
        this.codgFamilia = codgFamilia;
    }

    public Timestamp getDataHoraPartida() {
        return dataHoraPartida;
    }

    public void setDataHoraPartida(Timestamp dataHoraPartida) {
        this.dataHoraPartida = dataHoraPartida;
    }

    public Timestamp getDataHoraChegada() {
        return dataHoraChegada;
    }

    public void setDataHoraChegada(Timestamp dataHoraChegada) {
        this.dataHoraChegada = dataHoraChegada;
    }

    public int getTipoRota() {
        return tipoRota;
    }

    public void setTipoRota(int tipoRota) {
        this.tipoRota = tipoRota;
    }

    public int getQtdBagagem() {
        return qtdBagagem;
    }

    public void setQtdBagagem(int qtdBagagem) {
        this.qtdBagagem = qtdBagagem;
    }

    public double getPesoBagagem() {
        return pesoBagagem;
    }

    public void setPesoBagagem(double pesoBagagem) {
        this.pesoBagagem = pesoBagagem;
    }

    public CompanhiaAerea getCodgCompanhiaAereaOperada() {
        return codgCompanhiaAereaOperada;
    }

    public void setCodgCompanhiaAereaOperada(CompanhiaAerea codgCompanhiaAereaOperada) {
        this.codgCompanhiaAereaOperada = codgCompanhiaAereaOperada;
    }

    public CompanhiaAerea getCodgCompanhiaAereaCodeShare() {
        return codgCompanhiaAereaCodeShare;
    }

    public void setCodgCompanhiaAereaCodeShare(CompanhiaAerea codgCompanhiaAereaCodeShare) {
        this.codgCompanhiaAereaCodeShare = codgCompanhiaAereaCodeShare;
    }

    public int getFlagCodeShare() {
        return flagCodeShare;
    }

    public void setFlagCodeShare(int flagCodeShare) {
        this.flagCodeShare = flagCodeShare;
    }

    public String getStatusVoo() {
        return statusVoo;
    }

    public void setStatusVoo(String statusVoo) {
        this.statusVoo = statusVoo;
    }

    public String getLocalizadorCia() {
        return localizadorCia;
    }

    public void setLocalizadorCia(String localizadorCia) {
        this.localizadorCia = localizadorCia;
    }

    public String getCabine() {
        return cabine;
    }

    public void setCabine(String cabine) {
        this.cabine = cabine;
    }

}

