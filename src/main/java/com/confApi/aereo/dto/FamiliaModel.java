package com.confApi.aereo.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FamiliaModel {
    private Integer idOrdenacao =0;
    private Integer codgCompanhia;
    private String siglaCompanhia;
    private String nomeFamilia;
    private String descFamilia;
    private String codgFamilia;
    private String color;
    private Integer tipoRota;
    private List<FamiliaDetalheModel> familiaDetalhes = new ArrayList<>();
}
