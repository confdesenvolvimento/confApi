package com.confApi.hub.aereo;

import lombok.Data;

@Data
public class BagagemHub {
    private String id;
    private Integer quantidade;
    private Double valor;
    private Integer peso;
    private String unidadeDeMedida;
}
