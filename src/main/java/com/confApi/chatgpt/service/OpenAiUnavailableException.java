package com.confApi.chatgpt.service;

import java.io.IOException;

/** Código de diagnóstico local; nunca inclui corpo, chave ou cabeçalhos do provedor. */
public class OpenAiUnavailableException extends IOException {
    public OpenAiUnavailableException(String code) { super(code); }
}
