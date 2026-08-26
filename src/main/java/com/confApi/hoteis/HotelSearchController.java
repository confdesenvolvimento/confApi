package com.confApi.hoteis;

import com.confApi.db.confManager.hotel.model.HotelResponse;
import com.confApi.hoteis.model.pesquisa.HotelPesquisaModelFront;
import com.confApi.hoteis.model.reserva.CancelarReservaRequestHotelFront;
import com.confApi.hoteis.model.reserva.HotelCarregaModelFront;
import com.confApi.hoteis.model.reserva.ReservarRequestFront;
import com.confApi.hub.hotel.dto.HotelReserva;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/hoteis")
public class HotelSearchController {

    private final HotelSearchService service;
    private final HotelCityService hotelCityService;

    public HotelSearchController(HotelSearchService service, HotelCityService hotelCityService) {
        this.service = service;
        this.hotelCityService = hotelCityService;
    }

    @GetMapping("/cidades")
    public List<CidadeHotelQuantidadeDTO> buscarCidades(@RequestParam String query) {
        return hotelCityService.buscarCidadesComQuantidadeHoteis(query);
    }

    @PostMapping("/pesquisar")
    public List<HotelResponse> pesquisar(@RequestBody HotelPesquisaModelFront req) {
        return service.pesquisar(req);
    }

    @PostMapping("/efetuarReserva")
    public HotelReserva efetuarReserva(@RequestBody ReservarRequestFront req) {
        return service.efetuarReserva(req);
    }

    @PostMapping("/efetuarReserva2")
    public HotelReserva efetuarReserva2(@RequestBody ReservarRequestFront req, @RequestParam String idAgencia) {
        return service.efetuarReserva2(req, idAgencia);
    }

    @PostMapping("/carregarReserva")
    public HotelReserva carregarReserva(@RequestBody HotelCarregaModelFront req) {
        return service.carregarReserva(req);
    }

    @PostMapping("/cancelaHotel")
    public String cancelarReserva(@RequestBody CancelarReservaRequestHotelFront req) {
        return service.cancelarReserva(req);
    }
}
