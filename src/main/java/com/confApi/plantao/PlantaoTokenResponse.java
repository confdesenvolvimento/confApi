package com.confApi.plantao;

public record PlantaoTokenResponse(boolean success, String token, String message) {

    public static PlantaoTokenResponse sucesso(String token) {
        return new PlantaoTokenResponse(true, token, null);
    }

    public static PlantaoTokenResponse falha(String message) {
        return new PlantaoTokenResponse(false, null, message);
    }
}
