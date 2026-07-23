package com.confApi.hoteis.eNums;

public enum StatusReservaHotel {
    Confirmada(1, "Confirmada"),
    Cancelada(2, "Cancelada"),
    Rejeitada(3, "Rejeitada"),
    Modificada(4, "Modificada");




    private final int valor;
    private final String descricao;

    StatusReservaHotel(int valor, String descricao) {
        this.valor = valor;
        this.descricao = descricao;
    }

    public int getValor() {
        return valor;
    }

    public String getDescricao() {
        return descricao;
    }

    public static StatusReservaHotel fromValor(int valor) {
        for (StatusReservaHotel tipoStatus : StatusReservaHotel.values()) {
            if (tipoStatus.getValor() == valor) {
                return tipoStatus;
            }
        }
        throw new IllegalArgumentException("Valor de status inválido: " + valor);
    }

}
