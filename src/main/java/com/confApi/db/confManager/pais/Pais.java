package com.confApi.db.confManager.pais;

import com.confApi.db.confManager.continente.Continente;

import java.io.Serializable;

public class Pais implements Serializable {

    private Integer codgPais;

    private String nomePais;

    private String iataPais;

    public Pais() {
    }

    public Pais(String iataPais) {
        this.iataPais = iataPais;
    }



    public Pais(Integer codgPais) {
        this.codgPais = codgPais;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pais)) return false;
        Pais other = (Pais) o;
        return iataPais != null && iataPais.equalsIgnoreCase(other.iataPais);
    }
    @Override public int hashCode() {
        return iataPais == null ? 0 : iataPais.toUpperCase().hashCode();
    }





    private Continente continente;

    public Integer getCodgPais() {
        return codgPais;
    }

    public void setCodgPais(Integer codgPais) {
        this.codgPais = codgPais;
    }

    public String getNomePais() {
        return nomePais;
    }

    public void setNomePais(String nomePais) {
        this.nomePais = nomePais;
    }

    public String getIataPais() {
        return iataPais;
    }

    public void setIataPais(String iataPais) {
        this.iataPais = iataPais;
    }

    public Continente getContinente() {
        return continente;
    }

    public void setContinente(Continente continente) {
        this.continente = continente;
    }


}
