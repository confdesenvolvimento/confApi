package com.confApi.aereo.dto;

import lombok.Data;

@Data
public class PassageiroTipoQtd {
    private String tipo;
    private Integer quantidade;

    public PassageiroTipoQtd() {
    }

    public PassageiroTipoQtd(String tipo, Integer quantidade) {
        this.tipo = tipo;
        this.quantidade = quantidade;
    }
}
