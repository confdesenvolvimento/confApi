package com.confApi.db.confManager.hotel.model;

import java.io.Serializable;

public class PontoInteresse implements Serializable {
    private String nomePontoInterresse;
    private String latitude;
    private String longitude;
    private String descricao;
    private Double valorDistancia;




    public String getNomePontoInterresse() {
        return nomePontoInterresse;
    }

    public void setNomePontoInterresse(String nomePontoInterresse) {
        this.nomePontoInterresse = nomePontoInterresse;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Double getValorDistancia() {
        return valorDistancia;
    }

    public void setValorDistancia(Double valorDistancia) {
        this.valorDistancia = valorDistancia;
    }
}

