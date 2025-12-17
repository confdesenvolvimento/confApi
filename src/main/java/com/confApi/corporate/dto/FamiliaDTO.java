package com.confApi.corporate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
@Data
public class FamiliaDTO {
    private String nomeFamiliaCompanhia;
    private String nomeFamiliaCompanhiaDescricao;
    private String codSigla;
    private String corFamilia;
    private String rota;
    @JsonProperty("informacoes")
    private List<InformacoesFamiliaDTO> familiaDTOList;
}
