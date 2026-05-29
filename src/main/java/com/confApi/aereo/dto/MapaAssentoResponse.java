package com.confApi.aereo.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
public class MapaAssentoResponse {
    private String data;
    private String dataVersao;
    private Boolean sessaoExpirada;
    private Object exception;
    private List<PassageiroAssento> passageiros;
    private List<TrechoMapaAssento> trechos;

    private List<AssentoMarcar> assentosMarcados = new ArrayList<>();
}
