package com.confApi.carros.dto;

import lombok.Data;

import java.util.List;

@Data
public class GrupoCarros {
    private String nome;
    private String logo;
    private List<Carros> carros  = new java.util.ArrayList<>();
}
