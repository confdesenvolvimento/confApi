package com.confApi.db.confManager.exception;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MensagemDeErro {
    private int codigo;
    private String mensagem;

    public MensagemDeErro(int codigo, String mensagem) {
        this.codigo = codigo;
        this.mensagem = mensagem;
    }
}

