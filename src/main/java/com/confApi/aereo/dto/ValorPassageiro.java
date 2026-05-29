package com.confApi.aereo.dto;

import lombok.Data;

@Data
public class ValorPassageiro {
    private String nomePassageiro;
    private String cpf;
    private Double tarifa;
    private Double taxaEmbarque;
    private Double taxaDU;
    private Double RAV;
    private Double RC;
    private Double MKP;
    private Double total;
    private Double taxaAssento = 0.0;
}
