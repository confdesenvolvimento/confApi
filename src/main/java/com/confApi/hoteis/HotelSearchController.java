package com.confApi.hoteis;

import com.confApi.db.confManager.hotel.model.HotelResponse;
import com.confApi.hoteis.model.pesquisa.HotelPesquisaModelFront;
import com.confApi.hoteis.model.reserva.CancelarReservaRequestHotelFront;
import com.confApi.hoteis.model.reserva.HotelCarregaModelFront;
import com.confApi.hoteis.model.reserva.ReservarRequestFront;
import com.confApi.hub.hotel.dto.HotelReserva;
import org.springframework.web.bind.annotation.PathVariable;
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

    @org.springframework.beans.factory.annotation.Autowired
    private HotelPesquisaProgressivaService progressiva;

    private String dono(java.security.Principal principal) {
        if (principal == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        return principal.getName();
    }
    @PostMapping("/pesquisas")
    public HotelPesquisaProgressivaService.Pagina iniciar(@RequestBody HotelPesquisaModelFront req, java.security.Principal principal) {
        return progressiva.iniciar(dono(principal), req);
    }
    @GetMapping("/pesquisas/{id}")
    public HotelPesquisaProgressivaService.Pagina pagina(@PathVariable String id, @RequestParam(defaultValue="0") int cursor, java.security.Principal principal) {
        return progressiva.pagina(dono(principal), id, cursor);
    }
    public record ConteudoPedido(String fornecedor, String codigo) {}
    @PostMapping("/pesquisas/{id}/conteudo")
    public HotelResponse conteudo(@PathVariable String id, @RequestBody ConteudoPedido pedido, java.security.Principal principal) {
        if (pedido.fornecedor() == null || pedido.codigo() == null)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);
        return progressiva.conteudo(dono(principal), id, pedido.fornecedor(), pedido.codigo());
    }

    @PostMapping("/pesquisas/{id}/parar")
    public void parar(@PathVariable String id, java.security.Principal principal) {
        progressiva.parar(dono(principal), id);
    }
    @PostMapping("/pesquisas/{id}/cancelar")
    public void cancelar(@PathVariable String id, java.security.Principal principal) {
        progressiva.cancelar(dono(principal), id);
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
