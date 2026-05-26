package com.confApi.carros;

import com.confApi.db.confManager.carro.reserva.CarroReservaService;
import com.confApi.db.confManager.historicoReserva.HistoricoReservaService;
import org.springframework.stereotype.Service;

@Service
public class CarrosService {
    private final CarroReservaService carroReservaService;
    private final HistoricoReservaService historicoReservaService;

    public CarrosService(CarroReservaService carroReservaService, HistoricoReservaService historicoReservaService) {
        this.carroReservaService = carroReservaService;
        this.historicoReservaService = historicoReservaService;
    }
}
