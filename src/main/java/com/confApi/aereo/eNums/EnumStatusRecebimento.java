package com.confApi.aereo.eNums;

public enum EnumStatusRecebimento {
    CANCELADO(0, "Cancelado"),
    PAGO(1, "Pago"),
    AGUARDANDO(2, "Aguardando"),
    PARCIAL(3, "Parcial"),
    RECUSADO(5, "Recusado");

    private final int valor;
    private final String descricao;

    EnumStatusRecebimento(int valor, String descricao) {
        this.valor = valor;
        this.descricao = descricao;
    }

    public int getValor() {
        return valor;
    }

    public String getDescricao() {
        return descricao;
    }
}
