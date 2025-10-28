package com.confApi.hub.aereo.dto;

public class Bandeira {
    private Integer id;
    private String nome;

    public Bandeira() {
    }

    public Bandeira(Integer id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    @Override
    public String toString() {
        return "Bandeira{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                '}';
    }
}
