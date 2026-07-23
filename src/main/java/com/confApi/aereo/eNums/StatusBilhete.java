package com.confApi.aereo.eNums;

public enum StatusBilhete {
    Ativo(1),
    Cancelado(2),
    Indefinido(3);

    public Integer statusBilhete;

    private StatusBilhete(int statusBilhete) {
        this.statusBilhete = statusBilhete;
    }

    public static Integer getStatusPorValor(Integer valor) {
        for (StatusBilhete status : StatusBilhete.values()) {
            if (status.statusBilhete.equals(valor)) {
                return status.statusBilhete;
            }
        }
        return null; // Ou lança uma exceção, se preferir.
    }

    public static String getDescricaoPorValor(Integer valor) {
        for (StatusBilhete status : StatusBilhete.values()) {
            if (status.statusBilhete.equals(valor)) {
                return status.name();
            }
        }
        return null;
    }

    public static Integer getValorPorDescricao(String descricao) {
        if (descricao == null) {
            return null;
        }

        for (StatusBilhete status : StatusBilhete.values()) {
            if (status.name().equalsIgnoreCase(descricao)) {
                return status.statusBilhete;
            }
        }

        return null;
    }

}