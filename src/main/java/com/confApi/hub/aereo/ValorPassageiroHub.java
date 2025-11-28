package com.confApi.hub.aereo;

import lombok.Data;

@Data
public class ValorPassageiroHub {
    private String nomePassageiro;
    private String cpf;
    private Double tarifa = 0.0;
    private Double taxaEmbarque = 0.0;
    private Double taxaDU = 0.0;
    private Double RAV = 0.0;
    private Double RC;
    private Double MKP;
    private Double total = 0.0;
    private Double taxaAssento = 0.0;
}
