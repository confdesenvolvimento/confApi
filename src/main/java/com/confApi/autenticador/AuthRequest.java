package com.confApi.autenticador;

public record AuthRequest(
        String login,
        String token,
        Boolean info_agencia
) {}
