package com.confApi.endPoints.recebimento;

import lombok.Data;

@Data
public class CancelarResponse {
    private String objeto;
    private String mensagem;

    public CancelarResponse() {

    }

    public CancelarResponse(String objeto, String mensagem) {
        this.objeto = objeto;
        this.mensagem = mensagem;
    }
}
