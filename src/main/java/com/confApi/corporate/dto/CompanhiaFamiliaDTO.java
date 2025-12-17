package com.confApi.corporate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CompanhiaFamiliaDTO {
    private String nome;
    private String iata;
    private String numrCia;
    @JsonProperty("familias")
    private List<FamiliaDTO> familiaDTOList;
}
