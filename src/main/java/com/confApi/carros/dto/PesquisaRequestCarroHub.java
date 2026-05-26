package com.confApi.carros.dto;

import lombok.Data;

import java.util.List;

@Data
public class PesquisaRequestCarroHub {
    private VeiculoLocacaoHub retiradaVeiculo;
    private VeiculoLocacaoHub devolucaoVeiculo;
    private List<Integer> categorias;
    private List<String> iataCompanhias;
    private List<StoreCarroHub> stores;
    private List<SistemaCarroHub> sistemas;

    public PesquisaRequestCarroHub(PesquisaRequestCarroDTO obj) {
        this.retiradaVeiculo = obj.getRetiradaVeiculo();
        this.devolucaoVeiculo = obj.getDevolucaoVeiculo();
        this.categorias = obj.getCategorias();
        this.iataCompanhias = obj.getIataCompanhias();
        this.stores = obj.getStores();
        this.sistemas = obj.getSistemas();
    }
}
