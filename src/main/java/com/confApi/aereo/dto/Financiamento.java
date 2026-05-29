package com.confApi.aereo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Financiamento {
    @JsonProperty("Id")
    private int id;
    @JsonProperty("CoeficienteJuros")
    private int coeficienteJuros;
    @JsonProperty("DemaisParcela")
    private double demaisParcela;
    @JsonProperty("DemaisParcelas")
    private double demaisParcelas;
    @JsonProperty("Parcelas")
    private int parcelas;
    @JsonProperty("PrimeiraParcela")
    private double primeiraParcela;
    private double vlJurosCalculado;
}
