package com.confApi.db.confManager.carro.dto;

import com.confApi.db.confManager.carro.*;
import lombok.Data;

import java.util.List;

@Data
public class SalvarReservaCarroRequestDTO {

    private CarroReserva carroReserva;
    private Carro carro;
    private CarroValor carroValor;
    private List<CarroCondutor> condutores;
    private List<CarroItem> itens;
}
