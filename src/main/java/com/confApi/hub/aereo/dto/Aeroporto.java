package com.confApi.hub.aereo.dto;

import com.confApi.endPoints.aeroporto.AeroportoResponse;

import java.io.Serializable;
import java.util.Objects;

public class Aeroporto implements Serializable{
    private String codigoIata;
    private String descricao = "";

    public Aeroporto(AeroportoResponse aeroportoResponse) {
        this.codigoIata = aeroportoResponse.getCodigoIata();
        this.descricao = aeroportoResponse.getDescricao();
    }

    public Aeroporto() {
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.codigoIata);
        hash = 89 * hash + Objects.hashCode(this.descricao);
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
        final Aeroporto other = (Aeroporto) obj;
        if (!Objects.equals(this.codigoIata, other.codigoIata)) {
            return false;
        }
        return Objects.equals(this.descricao, other.descricao);
    }


    public Aeroporto(String codigoIata, String descricao) {
        this.codigoIata = codigoIata;
        this.descricao = descricao;
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
        return "Aeroporto{" +
                "codigoIata='" + codigoIata + '\'' +
                ", descricao='" + descricao + '\'' +
                '}';
    }
}

