package com.confApi.aereo.dto;

import lombok.Data;

@Data
public class ClasseSelecionada {
    private String baseTarifaria;
    private String classe;
    private String familia;
    private String numero;
    private String identificacaoDeVoo;
    private Integer trecho;
    private String sistema;
}
