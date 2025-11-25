package com.confApi.db.confManager.uf;

import com.confApi.db.confManager.pais.Pais;

import java.io.Serializable;

public class Uf implements Serializable {

    private Integer codgUf;

    private String nome;

    private String siglaUf;

    private Pais codgPais;

    public Integer getCodgUf() {
        return codgUf;
    }

    public void setCodgUf(Integer codgUf) {
        this.codgUf = codgUf;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSiglaUf() {
        return siglaUf;
    }

    public void setSiglaUf(String siglaUf) {
        this.siglaUf = siglaUf;
    }

    public Pais getCodgPais() {
        return codgPais;
    }

    public void setCodgPais(Pais codgPais) {
        this.codgPais = codgPais;
    }



}

