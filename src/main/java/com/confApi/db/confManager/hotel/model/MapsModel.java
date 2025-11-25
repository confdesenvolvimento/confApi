package com.confApi.db.confManager.hotel.model;

import java.io.Serializable;
import java.util.Locale;

public class MapsModel implements Serializable {
    private double latitude;
    private double longitude;
    private String nomeHotel;

    public MapsModel(double latitude, double longitude, String nomeHotel) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.nomeHotel = nomeHotel;
    }


    @Override
    public String toString() {
        // Normaliza o nome do hotel, removendo ou substituindo caracteres problemáticos
        String nomeHotelNormalizado = nomeHotel
                .replace("'", "\\'") // Escapa aspas simples
                .replace("\"", "\\\"") // Escapa aspas duplas
                .replace("\n", "") // Remove quebras de linha
                .replace("\r", "") // Remove retornos de carro
                .trim(); // Remove espaços extras nas extremidades

        return String.format(Locale.US,
                "{ latitude: %.5f, longitude: %.5f, nomeHotel: '%s' }",
                latitude, longitude, nomeHotelNormalizado);
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getNomeHotel() {
        return nomeHotel;
    }

    public void setNomeHotel(String nomeHotel) {
        this.nomeHotel = nomeHotel;
    }



}
