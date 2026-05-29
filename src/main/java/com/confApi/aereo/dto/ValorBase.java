package com.confApi.aereo.dto;

import lombok.Data;

import java.util.List;
@Data
public class ValorBase {
    private Double tarifa;
    private Double cambio;
    private Double taxaEmbarque;
    private Double taxaDU;
    private Double RAV;
    private Double RC;
    private Double MKP;
    private Double total;
    private List<ValorPassageiro> valorPassageiroList;
    private Double taxaAssento;
}
