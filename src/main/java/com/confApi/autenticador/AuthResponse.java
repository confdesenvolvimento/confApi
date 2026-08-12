package com.confApi.autenticador;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        Boolean success,
        String nome,
        Integer status,
        String message,
        @JsonProperty("info_agencia") String infoAgencia
) {
    public AuthResponse(Boolean success, String nome, Integer status, String message) {
        this(success, nome, status, message, null);
    }

    public static AuthResponse success(AuthResponse authResponse) {
        return authResponse;
    }

    public static AuthResponse invalid() {
        return new AuthResponse(false, null, null, "Credenciais inválidas");
    }
}
