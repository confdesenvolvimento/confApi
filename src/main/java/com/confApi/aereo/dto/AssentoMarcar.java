package com.confApi.aereo.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class AssentoMarcar {
    private String assentoColuna;
    private String assentoLinha;
    private String paxId;
    private String trechoId;
    private String assentoId;

    @JsonIgnore
    private Double valor =0.0;
}
