package com.confApi.hub.enumerador;

public enum OrdenacaoHub {
    VALOR(0,"Valor") ,
    VALORSEMBAGAGEM(1,"Valor sem bagagem") ,
    VALORCOMBAGAGEM(2,"Valor com bagagem") ,
    SAIDA(3,"Saída") ,
    CONEXOES(4,"Conexões") ,
    CHEGADA(5,"Chegada") ,
    DURACAO(6,"Duração");

    private Integer cod;
    private String descricao;

    OrdenacaoHub(Integer cod, String descricao) {
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

    public static OrdenacaoHub toEnum(Integer cod) {
        if (cod == null) {
            return null;
        }

        for (OrdenacaoHub x : OrdenacaoHub.values()) {
            if (cod.equals(x.getCod())) {
                return x;
            }
        }
        throw new IllegalArgumentException("ID inválido: " + cod);
    }
}
