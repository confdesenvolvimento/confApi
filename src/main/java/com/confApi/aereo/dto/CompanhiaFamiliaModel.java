package com.confApi.aereo.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CompanhiaFamiliaModel {
    private String nomeCompanhia;
    private String siglaCompanhia;
    List<FamiliaModel> familias = new ArrayList<>();

    public CompanhiaFamiliaModel(String siglaCompanhia) {
        this.siglaCompanhia = siglaCompanhia;
    }

    public CompanhiaFamiliaModel(String nomeCompanhia, String siglaCompanhia) {
        this.nomeCompanhia = nomeCompanhia;
        this.siglaCompanhia = siglaCompanhia;

    }
}
