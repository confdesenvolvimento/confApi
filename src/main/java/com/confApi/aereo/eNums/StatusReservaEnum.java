package com.confApi.aereo.eNums;

public enum StatusReservaEnum {
    Ativa(1, "Ativa"),
    Cancelada(2, "Cancelada"),
    Emitida(3, "Emitida");



    private final int valor;
    private final String descricao;

    StatusReservaEnum(int valor, String descricao) {
        this.valor = valor;
        this.descricao = descricao;
    }

    public int getValor() {
        return valor;
    }

    public String getDescricao() {
        return descricao;
    }

    public static StatusReservaEnum fromValor(int valor) {
        for (StatusReservaEnum tipoStatus : StatusReservaEnum.values()) {
            if (tipoStatus.getValor() == valor) {
                return tipoStatus;
            }
        }
        throw new IllegalArgumentException("Valor de status inválido: " + valor);
    }

    public static StatusReservaEnum fromNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Status da reserva não informado");
        }

        for (StatusReservaEnum tipoStatus : StatusReservaEnum.values()) {
            if (tipoStatus.getDescricao().equalsIgnoreCase(nome.trim())) {
                return tipoStatus;
            }
        }

        throw new IllegalArgumentException("Valor de status inválido: " + nome);
    }
}
