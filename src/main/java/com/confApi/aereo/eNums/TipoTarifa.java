package com.confApi.aereo.eNums;

public enum TipoTarifa {
    RT(0,"Round Trip"),
    OW(1,"One Way");

    private Integer cod;
    private String descricao;

    TipoTarifa(Integer cod, String descricao) {
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

    public static TipoTarifa toEnum(Integer cod) {
        if (cod == null) {
            return null;
        }

        for (TipoTarifa x : TipoTarifa.values()) {
            if (cod.equals(x.getCod())) {
                return x;
            }
        }
        throw new IllegalArgumentException("ID inválido: " + cod);
    }
}