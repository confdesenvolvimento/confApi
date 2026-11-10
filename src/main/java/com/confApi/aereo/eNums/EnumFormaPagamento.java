package com.confApi.aereo.eNums;

public enum EnumFormaPagamento {
    FATURADO(1, "Faturado"),
    CARTAO(2, "Cartao"),
    ENTRADA_CARTAO(3, "ENTRADA + CARTAO"),
    PIX(4, "Pix"),
    LINK_PAGAMENTO(5, "LINK PAGAMENTO - CARTAO");


    private final int valor;
    private final String descricao;


    EnumFormaPagamento(int valor, String descricao) {
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
