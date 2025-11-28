package com.confApi.endPoints.passaporte;

import lombok.Data;

@Data
public class PassaporteResponse {
    private String nacionalidade;
    private String nomeDoMeioPax;
    private String nomePax;
    private String numero;
    private String paisEmissor;
    private String sobrenomePax;
    private String validade;
}
