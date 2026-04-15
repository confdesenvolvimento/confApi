package com.confApi.hub.telegram.dto;

import lombok.Data;

@Data
public class MensagemRequest {

    private String classe;
    private String mensagem;
    private String metodo;
    private String projeto;

    public MensagemRequest(String mensagem) {
        this.mensagem = mensagem;
    }

    public MensagemRequest() {
    }
}
