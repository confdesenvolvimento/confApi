package com.confApi.carros.dto;

import lombok.Data;

import java.util.List;

@Data
public class PesquisaRequestCarroDTO {
    private VeiculoLocacaoHub retiradaVeiculo;
    private VeiculoLocacaoHub devolucaoVeiculo;
    private List<Integer> categorias;
    private List<String> iataCompanhias;
    private List<StoreCarroHub> stores;
    private List<SistemaCarroHub> sistemas;
}
