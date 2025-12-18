package com.confApi.db.confManager.usuario.dto.autenticar;

import lombok.Data;

import java.time.LocalDate;
@Data
public class UsuarioDTO {
    private String nome;
    private String email;
    private String telefone;
    private String celular;
    private LocalDate nascimento;
    private String cpf;

    private Boolean podeEmitir;
    private String nomeCompleto;
    private String primeiroNome;
    private String ultimoNome;

    private String sexo;
    private String tipoCorporativo;
    private String grupo;
    private String tipo;
    private String tokenIdentificacao;

    public UsuarioDTO() {}

    public UsuarioDTO(String nome, String email, String telefone, String celular, LocalDate nascimento, String cpf,
                      Boolean podeEmitir, String nomeCompleto, String primeiroNome, String ultimoNome,
                      String sexo, String tipoCorporativo, String grupo, String tipo, String tokenIdentificacao) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.celular = celular;
        this.nascimento = nascimento;
        this.cpf = cpf;
        this.podeEmitir = podeEmitir;
        this.nomeCompleto = nomeCompleto;
        this.primeiroNome = primeiroNome;
        this.ultimoNome = ultimoNome;
        this.sexo = sexo;
        this.tipoCorporativo = tipoCorporativo;
        this.grupo = grupo;
        this.tipo = tipo;
        this.tokenIdentificacao = tokenIdentificacao;
    }

    // getters/setters
}