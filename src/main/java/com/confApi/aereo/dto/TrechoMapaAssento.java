package com.confApi.aereo.dto;

import lombok.Data;

@Data
public class TrechoMapaAssento {
    private String companhia;
    private String dataDeSaida;
    private String destino;
    private String equipamento;
    private MapaAssento mapaDeAssentos;
    private String numeroDoVoo;
    private String origem;
    private String trechoID;
    private String paxID;
    private Boolean isExibirMapaAssento = false;
}
