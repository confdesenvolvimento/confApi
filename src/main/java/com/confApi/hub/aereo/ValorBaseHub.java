package com.confApi.hub.aereo;

import lombok.Data;

import java.util.List;

@Data
public class ValorBaseHub {
    private Double tarifa;
    private Double cambio;
    private Double taxaEmbarque;
    private Double taxaDU;
    private Double RAV;
    private Double RC;
    private Double MKP;
    private Double total;
    private List<ValorPassageiroHub> valorPassageiroList;
    private Double taxaAssento;
}
