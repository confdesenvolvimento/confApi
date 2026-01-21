package com.confApi.seguros.eNums;

public enum EstadoCivil {
    SOLTEIRO(1,"Solteiro") ,
    CASADO(2,"Casado") ,
    SEPARADO(3,"Sparado"),
    DIVORCIADO(4,"Divorciado"),
    VIUVO(5,"Víuvo"),
    OUTROS(6,"Outros");

    private Integer cod;
    private String descricao;

    EstadoCivil(Integer cod, String descricao) {
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

    public static EstadoCivil toEnum(Integer cod) {
        if (cod == null) {
            return null;
        }

        for (EstadoCivil x : EstadoCivil.values()) {
            if (cod.equals(x.getCod())) {
                return x;
            }
        }
        throw new IllegalArgumentException("ID inválido: " + cod);
    }
}
