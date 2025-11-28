package com.confApi.hub.enumerador;

public enum ClasseHub {
    DEFAULT(0,"Padrão", "") ,
    ECONOMICA(1,"Econômica", "Y") ,
    ECONOMICA_PREMIUM(2,"Econômica Premium", "W"),
    EXECUTIVA(3,"Executiva", "C"),
    PRIMEIRA_CLASSE(4,"Primeira Classe", "F"),
    ECONOMICAPLUSPREMIUM(5,"Econômica + Premium", "P");

    private Integer cod;
    private String descricao;
    private String abreviacao;

    ClasseHub(Integer cod, String descricao, String abreviacao) {
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

    public static ClasseHub toEnum(Integer cod) {
        if (cod == null) {
            return null;
        }

        for (ClasseHub x : ClasseHub.values()) {
            if (cod.equals(x.getCod())) {
                return x;
            }
        }
        throw new IllegalArgumentException("ID inválido: " + cod);
    }
}
