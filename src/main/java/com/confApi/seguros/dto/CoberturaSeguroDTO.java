package com.confApi.seguros.dto;

import lombok.Data;

import java.io.Serializable;

import static org.apache.logging.log4j.util.Strings.isBlank;

@Data
public class CoberturaSeguroDTO implements Serializable {
    private Integer orderDisplay =0;
    private String nome;
    private String descricao;
    private Double valor=0.0;
    private String moeda = "USD";

    // opcional UI
    private String icone; // ex: "pi pi-heart"
    private String obs;   // texto curto

    public String getIcone() {
        return isBlank(icone) ? "pi-shield" : icone;
    }
}
