package com.confApi.hub.aereo.dto;

import java.io.Serializable;


public class Contato implements Serializable{
    private String cidade;
    private String email;
    private String endereco;
    private String numeroDDD;
    private String numeroDDI;
    private String numeroTelefone;
    private String nome;


    public Contato() {
    }

    public Contato(String cidade, String email, String endereco, String numeroDDD,
                   String numeroDDI, String numeroTelefone, String nome) {
        this.cidade = cidade;
        this.email = email;
        this.endereco = endereco;
        this.numeroDDD = numeroDDD;
        this.numeroDDI = numeroDDI;
        this.numeroTelefone = numeroTelefone;
        this.nome = nome;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getNumeroDDD() {
        return numeroDDD;
    }

    public void setNumeroDDD(String numeroDDD) {
        this.numeroDDD = numeroDDD;
    }

    public String getNumeroDDI() {
        return numeroDDI;
    }

    public void setNumeroDDI(String numeroDDI) {
        this.numeroDDI = numeroDDI;
    }

    public String getNumeroTelefone() {
        return numeroTelefone;
    }

    public void setNumeroTelefone(String numeroTelefone) {
        this.numeroTelefone = numeroTelefone;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}

