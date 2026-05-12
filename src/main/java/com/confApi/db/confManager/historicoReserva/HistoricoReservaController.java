package com.confApi.db.confManager.historicoReserva;

import com.confApi.db.confManager.historicoReserva.dto.HistoricoReserva;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("manager/historico/reserva")
public class HistoricoReservaController {

    @Autowired
    private HistoricoReservaService historicoReservaService;

    @GetMapping("/findByCodgReservaAereo/{codgReservaAereo}")
    public List<HistoricoReserva> findByCodgReservaAereo(@PathVariable Integer codgReservaAereo){
        return historicoReservaService.findByCodgReservaAereo(codgReservaAereo);
    }

    @GetMapping("/findByCodgReservaHotel/{codgReservaHotel}")
    public List<HistoricoReserva> findByCodgReservaHotel(@PathVariable Integer codgReservaHotel) {
        return historicoReservaService.findByCodgReservaHotel(codgReservaHotel);
    }

    @GetMapping("/findByCodgReservaSeguro/{codgReservaSeguro}")
    public List<HistoricoReserva> findByCodgReservaSeguro(@PathVariable Integer codgReservaSeguro) {
        return historicoReservaService.findByCodgReservaSeguro(codgReservaSeguro);
    }

    @GetMapping("/findByCodgReservaPacote/{codgReservaPacote}")
    public List<HistoricoReserva> findByCodgReservaPacote(@PathVariable Integer codgReservaPacote) {
        return historicoReservaService.findByCodgReservaPacote(codgReservaPacote);
    }


}
