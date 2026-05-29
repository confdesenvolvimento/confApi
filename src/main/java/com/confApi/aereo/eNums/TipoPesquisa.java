package com.confApi.aereo.eNums;

public enum TipoPesquisa {
    ROUNDTRIP(0,"Round Trip") ,
    ONEWAY(1,"One Way"),
    MULTIPLOSTRECHOS(2,"Multiplos Trechos");

    private Integer cod;
    private String descricao;

    TipoPesquisa(Integer cod, String descricao) {
        this.cod = cod;
        this.descricao = descricao;
    }
    public static TipoPesquisa fromCod(Integer cod) {
        if (cod == null) {
            return null;
        }

        for (TipoPesquisa tipo : values()) {
            if (tipo.getCod().equals(cod)) {
                return tipo;
            }
        }

        throw new IllegalArgumentException("Código inválido para TipoConsulta: " + cod);
    }
    public Integer getCod() {
        return cod;
    }

    public void setCod(Integer cod) {
        this.cod = cod;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public static TipoPesquisa toEnum(Integer cod) {
        if (cod == null) {
            return null;
        }

        for (TipoPesquisa x : TipoPesquisa.values()) {
            if (cod.equals(x.getCod())) {
                return x;
            }
        }
        throw new IllegalArgumentException("ID inválido: " + cod);
    }


    @Override
    public String toString() {
        return "TipoPesquisa{" +
                "cod=" + cod +
                ", descricao='" + descricao + '\'' +
                '}';
    }
    }
