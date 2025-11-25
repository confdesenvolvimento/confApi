package com.confApi.db.confManager.continente;

import java.io.Serializable;

public class Continente implements Serializable {

    private Integer codgContinente;

    private String nomeContinente;

    private String iataContinente;

    public Integer getCodgContinente() {
        return codgContinente;
    }

    public void setCodgContinente(Integer codgContinente) {
        this.codgContinente = codgContinente;
    }

    public String getNomeContinente() {
        return nomeContinente;
    }

    public void setNomeContinente(String nomeContinente) {
        this.nomeContinente = nomeContinente;
    }

    public String getIataContinente() {
        return iataContinente;
    }

    public void setIataContinente(String iataContinente) {
        this.iataContinente = iataContinente;
    }



}