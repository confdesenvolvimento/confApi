package com.confApi.db.enumeradores;

public enum TipoConsulta {
    NACIONAL(0,"Nacional") ,
    INTERNACIONAL(1,"Internacional");

    private Integer cod;
    private String descricao;

    TipoConsulta(Integer cod, String descricao) {
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

    public static TipoConsulta toEnum(Integer cod) {
        if (cod == null) {
            return null;
        }

        for (TipoConsulta x : TipoConsulta.values()) {
            if (cod.equals(x.getCod())) {
                return x;
            }
        }
        throw new IllegalArgumentException("ID inválido: " + cod);
    }

    @Override
    public String toString() {
        return "TipoConsulta{" +
                "cod=" + cod +
                ", descricao='" + descricao + '\'' +
                '}';
    }
}
