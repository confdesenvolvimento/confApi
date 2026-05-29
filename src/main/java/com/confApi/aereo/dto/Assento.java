package com.confApi.aereo.dto;

import lombok.Data;

@Data
public class Assento {
    private String assentoColuna;
    private String assentoLinha;
    private String paxID;
    private String trechoID;
    private String voo;
    private String passageiro;
    private Double valor;
    private String trecho;
}
