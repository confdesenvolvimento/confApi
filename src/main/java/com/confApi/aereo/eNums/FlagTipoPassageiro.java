package com.confApi.aereo.eNums;

public enum FlagTipoPassageiro {
    ADT(1),
    CHD(2),
    INF(3);
    private Integer tipoPax;

    private FlagTipoPassageiro(Integer tipoPax) {
        this.tipoPax = tipoPax;
    }

    public static Integer getValor(String tipoPassageiro) {
        if ("ADT".equals(tipoPassageiro)) {
            return FlagTipoPassageiro.ADT.tipoPax;
        } else if ("CHD".equals(tipoPassageiro)) {
            return FlagTipoPassageiro.CHD.tipoPax;
        } else if ("INF".equals(tipoPassageiro)) {
            return FlagTipoPassageiro.INF.tipoPax;
        }
        // Se a string não corresponder a nenhum valor válido, você pode retornar null ou lançar uma exceção, dependendo do seu caso de uso.
        return null;
    }

    public Integer getTipoPax() {
        return tipoPax;
    }

    public void setTipoPax(Integer tipoPax) {
        this.tipoPax = tipoPax;
    }


}
