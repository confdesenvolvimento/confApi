package com.confApi.hub.enumerador;

public enum TipoPesquisaHub {
    ROUNDTRIP(0,"Round Trip") ,
    ONEWAY(1,"One Way"),
    MULTIPLOSTRECHOS(2,"Multiplos Trechos");

    private Integer cod;
    private String descricao;

    TipoPesquisaHub(Integer cod, String descricao) {
        this.cod = cod;
        this.descricao = descricao;
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

    public static TipoPesquisaHub toEnum(Integer cod) {
        if (cod == null) {
            return null;
        }

        for (TipoPesquisaHub x : TipoPesquisaHub.values()) {
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
