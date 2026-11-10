package com.confApi.aereo.eNums;

public enum StatusBilheteWooba {
    Ativa(1),
    Cancelada(2),
    Indefinida(3);

    public Integer statusBilhete;

    private StatusBilheteWooba(int statusBilhete) {
        this.statusBilhete = statusBilhete;
    }

    public static Integer getStatusPorValor(Integer valor) {
        for (StatusBilheteWooba status : StatusBilheteWooba.values()) {
            if (status.statusBilhete.equals(valor)) {
                return status.statusBilhete;
            }
        }
        return null; // Ou lança uma exceção, se preferir.
    }

    public static String getDescricaoPorValor(Integer valor) {
        for (StatusBilheteWooba status : StatusBilheteWooba.values()) {
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

        for (StatusBilheteWooba status : StatusBilheteWooba.values()) {
            if (status.name().equalsIgnoreCase(descricao)) {
                return status.statusBilhete;
            }
        }

        return null;
    }

}