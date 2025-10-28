package com.confApi.hub.aereo.dto;

import java.io.Serializable;
import java.util.Objects;

public class Companhia implements Serializable {
    private Integer id = 0;
    private String codigoIata = "";
    private String descricao = "";

    public Companhia() {
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + Objects.hashCode(this.id);
        hash = 53 * hash + Objects.hashCode(this.codigoIata);
        hash = 53 * hash + Objects.hashCode(this.descricao);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Companhia other = (Companhia) obj;
        if (!Objects.equals(this.codigoIata, other.codigoIata)) {
            return false;
        }
        if (!Objects.equals(this.descricao, other.descricao)) {
            return false;
        }
        return Objects.equals(this.id, other.id);
    }



    public Companhia(Integer id, String codigoIata, String descricao) {
        this.id = id;
        this.codigoIata = codigoIata;
        this.descricao = descricao;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodigoIata() {
        return codigoIata;
    }

    public void setCodigoIata(String codigoIata) {
        this.codigoIata = codigoIata;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String toString() {
        return "Companhia{" +
                "id=" + id +
                ", codigoIata='" + codigoIata + '\'' +
                ", descricao='" + descricao + '\'' +
                '}';
    }
}
