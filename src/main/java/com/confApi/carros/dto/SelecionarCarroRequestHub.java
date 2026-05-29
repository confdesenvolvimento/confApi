package com.confApi.carros.dto;

import lombok.Data;

import java.util.List;

@Data
public class SelecionarCarroRequestHub {
    private String index;
    private String supplier;
    private String token;
    private List<SistemaCarroHub> sistemas;

    public SelecionarCarroRequestHub(SelecionarCarroRequestDTO obj) {
        this.index = obj.getIndex();
        this.supplier = obj.getSupplier();
        this.token = obj.getToken();
        this.sistemas = obj.getSistemas();
    }
}
