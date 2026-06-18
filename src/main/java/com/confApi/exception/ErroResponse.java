package com.confApi.exception;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErroResponse {
    private int status;
    private String mensagem;
    private LocalDateTime timestamp = LocalDateTime.now();

    public ErroResponse(int status, String mensagem) {
        this.status = status;
        this.mensagem = mensagem;
    }
}
