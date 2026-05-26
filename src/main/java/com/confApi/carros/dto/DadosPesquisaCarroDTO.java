package com.confApi.carros.dto;

import lombok.Data;

import java.util.List;

@Data
public class DadosPesquisaCarroDTO {
    private LocalCarro retiradaVeiculo;
    private LocalCarro devolucaoVeiculo;
    private Integer numeroDias;
    private Double tempoPesquisa;
    private Integer numeroCarros;
    private List<GrupoCarros> carsGroups  = new java.util.ArrayList<>();
}
