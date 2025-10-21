package com.confApi.hub.enumerador;

public enum TipoLimite {
    FATURADO("FATURADO", "FATURADO"),
    GOVERNO("GOVERNO", "GOVERNO"),
    CARTAO_CREDITO("CARTAO_CREDITO", "CARTÃO DE CRÉDITO"),
    CARTAO_CREDITO_PROPRIO("CARTAO_CREDITO_PROPRIO", "CONFIANCA CARD");

    private final String value;
    private final String descricao;

    TipoLimite(String value, String descricao) {
        this.value = value;
        this.descricao = descricao;
    }

    public String getValue() {
        return value;
    }

    public String getDescricao() {
        return descricao;
    }
}