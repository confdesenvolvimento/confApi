package com.confApi.aereo.dto;

import lombok.Data;

@Data
public class Agencia {
    private String codgAgencia;
    private String login;
    private String senha;
    private String contato;
    private String email;
    private String nome;
    private String unidade;
    private String codgSistemaBackoffice = null;
}
