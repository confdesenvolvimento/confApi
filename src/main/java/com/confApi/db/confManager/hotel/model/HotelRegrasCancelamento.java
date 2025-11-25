package com.confApi.db.confManager.hotel.model;

import java.io.Serializable;

public class HotelRegrasCancelamento implements Serializable {

    private String codgReferencia;
    private String nomeRegra;
    private String descricaoRegra;
    private Boolean isReembolsavel;
    private Double valorMulta = 0.0;

    public String getCodgReferencia() {
        return codgReferencia;
    }

    public void setCodgReferencia(String codgReferencia) {
        this.codgReferencia = codgReferencia;
    }

    public Double getValorMulta() {
        return valorMulta;
    }

    public void setValorMulta(Double valorMulta) {
        this.valorMulta = valorMulta;
    }

    public String getNomeRegra() {
        return nomeRegra;
    }

    public void setNomeRegra(String nomeRegra) {
        this.nomeRegra = nomeRegra;
    }

    public String getDescricaoRegra() {
        return descricaoRegra;
    }

    public void setDescricaoRegra(String descricaoRegra) {
        this.descricaoRegra = descricaoRegra;
    }

    public Boolean getIsReembolsavel() {
        return isReembolsavel;
    }

    public void setIsReembolsavel(Boolean isReembolsavel) {
        this.isReembolsavel = isReembolsavel;
    }

}

