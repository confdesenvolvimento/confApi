package com.confApi.aereo.eNums;

import com.fasterxml.jackson.annotation.JsonCreator;

/** Modalidade comercial; independente do tipo de tarifa RT/OW do fornecedor. */
public enum ModalidadeTarifa {
    CONS,
    OPER;

    @JsonCreator
    public static ModalidadeTarifa fromJson(String value) {
        if ("CONS".equals(value)) return CONS;
        if ("OPER".equals(value)) return OPER;
        throw new IllegalArgumentException("Modalidade de tarifa invalida. Use CONS ou OPER.");
    }
}
