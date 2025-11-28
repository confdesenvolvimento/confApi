package com.confApi.endPoints.contato;

import lombok.Data;

@Data
public class ContatoResponse {
    private String cidade;
    private String email;
    private String endereco;
    private String numeroDDD;
    private String numeroDDI;
    private String numeroTelefone;
    private String nome;
}
