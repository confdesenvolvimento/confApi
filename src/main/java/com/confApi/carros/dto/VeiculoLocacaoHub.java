package com.confApi.carros.dto;

import lombok.Data;

@Data
public class VeiculoLocacaoHub {
    private String iata;
    private String tipoIata;
    private String dataHora;
    private String latitude;
    private String longitude;
}
