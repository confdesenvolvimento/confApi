package com.confApi.db.confManager.cidade;

import com.confApi.db.confManager.pais.Pais;
import com.confApi.db.confManager.uf.Uf;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Cidade implements Serializable {

    private Integer codgCidade;
    private String nomeCidade;
    private String iataCidade;
    private Pais pais;
    private Uf uf;

    public Cidade(Integer codgCidade) {
        this.codgCidade = codgCidade;
    }

    public Cidade() {
    }



    public Integer getCodgCidade() {
        return codgCidade;
    }

    public void setCodgCidade(Integer codgCidade) {
        this.codgCidade = codgCidade;
    }

    public String getNomeCidade() {
        return nomeCidade;
    }

    public void setNomeCidade(String nomeCidade) {
        this.nomeCidade = nomeCidade;
    }

    public String getIataCidade() {
        return iataCidade;
    }

    public void setIataCidade(String iataCidade) {
        this.iataCidade = iataCidade;
    }

    public Pais getPais() {
        return pais;
    }

    public void setPais(Pais pais) {
        this.pais = pais;
    }

    public Uf getUf() {
        return uf;
    }

    public void setUf(Uf uf) {
        this.uf = uf;
    }


}
