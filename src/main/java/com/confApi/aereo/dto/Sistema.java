package com.confApi.aereo.dto;

public class Sistema {
    private Integer codigo = 0;
    private String nome = "";
    private Integer sistema = 0;

    public Sistema() {
    }

    public Sistema(Integer codigo, String nome, Integer sistema) {
        this.codigo = codigo;
        this.nome = nome;
        this.sistema = sistema;
    }

    public Integer getCodigo() {
        return codigo;
    }

    public void setCodigo(Integer codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getSistema() {
        return sistema;
    }

    public void setSistema(Integer sistema) {
        this.sistema = sistema;
    }

    @Override
    public String toString() {
        return "Sistema{" +
                "codigo=" + codigo +
                ", nome='" + nome + '\'' +
                ", sistema=" + sistema +
                '}';
    }
}
