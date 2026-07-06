package com.confApi.carros.dto;

import com.confApi.db.confManager.carro.Carro;
import com.confApi.db.confManager.carro.CarroCondutor;
import com.confApi.db.confManager.carro.CarroReserva;
import com.confApi.db.confManager.carro.CarroValor;
import com.confApi.db.confManager.carro.CarroItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarroReservaDetalheDTO {

    private Integer codgReservaCarro;
    private CarroReserva carroReserva;
    private Carro carro;
    private CarroValor carroValor;
    private List<CarroCondutor> condutores = new ArrayList<>();
    private List<CarroItem> itens = new ArrayList<>();
}
