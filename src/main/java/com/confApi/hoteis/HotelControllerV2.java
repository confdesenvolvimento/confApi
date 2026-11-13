package com.confApi.hoteis;

import com.confApi.db.confManager.hotel.model.HotelPreReserva;
import com.confApi.db.confManager.hotel.model.HotelResponse;
import com.confApi.db.confManager.hotel.model.ReservaHotelModel;
import com.confApi.db.confManager.reservaHotel.dto.ReservaHotel;
import com.confApi.hoteis.model.pesquisa.HotelPesquisaModelFront;
import com.confApi.hoteis.model.reserva.CancelarReservaRequestHotelFront;
import com.confApi.hoteis.model.reserva.HotelCarregaModelFront;
import com.confApi.hub.hotel.dto.HotelReserva;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v2/hoteis")
public class HotelControllerV2 {

    private final HotelClientV2 service;

    public HotelControllerV2(HotelClientV2 service) {
        this.service = service;
    }

    @PostMapping("/pesquisar")
    public List<HotelResponse> pesquisar(@RequestBody HotelPesquisaModelFront req) {
        return service.pesquisar(req);
    }

    @PostMapping("/efetuarReserva")
    public ReservaHotel efetuarReserva(@RequestBody HotelPreReserva req) {
        return service.efetuarReserva(req);
    }

    @PostMapping("/carregarReserva")
    public HotelReserva carregarReserva(@RequestBody HotelCarregaModelFront req) {
        return service.carregarReserva(req);
    }

    @PostMapping("/cancelaHotel")
    public String cancelarReserva(@RequestBody ReservaHotelModel req) {
        return service.cancelarReserva(req);
    }
}
