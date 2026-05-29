package com.confApi.aereo.dto;

import lombok.Data;

import java.util.List;
@Data
public class Linha {
    private Boolean asa;
    private List<Coluna> colunas;
    private Boolean disponivel;
    private Boolean inicioDaAsa;
    private String linha;
    private Boolean saidaEmergencia;
    private Boolean terminoDaAsa;
}
