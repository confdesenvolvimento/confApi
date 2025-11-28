package com.confApi.hub.enumerador;

public enum TipoBagagemHub {
    TODAS_AS_OPCOES(0,"Todas as opções") ,
    APENAS_COM_BAGAGEM(1,"Apenas com bagagem"),
    APENAS_SEM_BAGAGEM(2,"Apenas sem bagagem");

    private Integer cod;
    private String descricao;

    TipoBagagemHub(Integer cod, String descricao) {
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

    public static TipoBagagemHub toEnum(Integer cod) {
        if (cod == null) {
            return null;
        }

        for (TipoBagagemHub x : TipoBagagemHub.values()) {
            if (cod.equals(x.getCod())) {
                return x;
            }
        }
        throw new IllegalArgumentException("ID inválido: " + cod);
    }
}
