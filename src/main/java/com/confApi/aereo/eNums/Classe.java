package com.confApi.aereo.eNums;

public enum Classe {
    ECONOMICA(0,"Econômica", "Y") ,
    ECONOMICA_PREMIUM(1,"Econômica Premium", "W"),
    EXECUTIVA(2,"Executiva", "C"),
    PRIMEIRA_CLASSE(3,"Primeira Classe", "F"),
    ECONOMICAPLUSPREMIUM(4,"Econômica + Premium", "P");

    private Integer cod;
    private String descricao;
    private String abreviacao;

    Classe(Integer cod, String descricao, String abreviacao) {
        this.cod = cod;
        this.descricao = descricao;
        this.abreviacao = abreviacao;
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

    public String getAbreviacao() {
        return abreviacao;
    }

    public void setAbreviacao(String abreviacao) {
        this.abreviacao = abreviacao;
    }

    public static Classe toEnum(Integer cod) {
        if (cod == null) {
            return null;
        }

        for (Classe x : Classe.values()) {
            if (cod.equals(x.getCod())) {
                return x;
            }
        }
        throw new IllegalArgumentException("ID inválido: " + cod);
    }

}
