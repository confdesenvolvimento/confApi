package com.confApi.hub.aereo;

import lombok.Data;

import java.util.Date;

@Data
public class PassaporteHub {
    private String nacionalidade;
    private String nomeDoMeioPax;
    private String nomePax;
    private String numero;
    private String paisEmissor;
    private String sobrenomePax;
    private Date validade;
}
