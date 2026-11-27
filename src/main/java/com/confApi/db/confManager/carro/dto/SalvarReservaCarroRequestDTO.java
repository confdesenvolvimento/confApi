package com.confApi.db.confManager.carro.dto;

import com.confApi.db.confManager.carro.*;
import lombok.Data;

import java.util.List;

@Data
public class SalvarReservaCarroRequestDTO {
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
    private java.util.List<com.confApi.operacao.DivergenciaOperacao> divergenciasOperacao = new java.util.ArrayList<>();


    private CarroReserva carroReserva;
    private Carro carro;
    private CarroValor carroValor;
    private List<CarroCondutor> condutores;
    private List<CarroItem> itens;
}
