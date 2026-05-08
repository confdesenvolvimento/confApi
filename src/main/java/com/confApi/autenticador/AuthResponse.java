package com.confApi.autenticador;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        Boolean success,
        String nome,
        Integer status,
        String message
) {
    public static AuthResponse success(AuthResponse authResponse) {
        return authResponse;
    }

    public static AuthResponse invalid() {
        return new AuthResponse(false, null, null, "Credenciais inválidas");
    }
}
