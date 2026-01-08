package com.confApi.seguros.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
@Data
public class SeguradoDTO implements Serializable {

    // identificação/apoio
    private int idade;
    private String faixaLabel; // "0 a 64 anos"

    // dados pessoais
    private String nome;
    private String sobrenome;
    private String sexo; // F / M
    private String nascimento;

    private String email;
    private String telefone;

    private String cpf;
    private boolean menorSemCpf;
    private String documento;

    // endereço
    private String cep;
    private String endereco;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
}
