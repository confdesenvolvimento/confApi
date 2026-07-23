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

    public Agencia() {
    }

    public Agencia(String codgAgencia, String login, String senha, String contato,
                   String email, String nome, String unidade, String codgSistemaBackoffice) {
        this.codgAgencia = codgAgencia;
        this.login = login;
        this.senha = senha;
        this.contato = contato;
        this.email = email;
        this.nome = nome;
        this.unidade = unidade;
        this.codgSistemaBackoffice = codgSistemaBackoffice;
    }

    public Agencia(com.confApi.db.confManager.agencia.dto.Agencia agencia) {
        this.codgAgencia = agencia.getCodgAgencia().toString();
        this.login = agencia.getUsuarioApi();
        this.senha = agencia.getSenhaApi();
        this.contato = agencia.getTelefone();
        this.email = agencia.getEmail();
        this.nome = agencia.getNomeAgencia();
        this.unidade = agencia.getUnidade() != null ? agencia.getUnidade().getNomeUnidade() : null;
        this.codgSistemaBackoffice = agencia.getCodgSistemaBackOffice();
    }
}
