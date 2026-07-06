package com.confApi.carros;

import com.confApi.carros.dto.*;
import com.confApi.db.confManager.carro.CarroReserva;
import com.confApi.db.confManager.carro.CarroReservaService;
import com.confApi.hub.carro.HubCarroClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/carro")
public class CarrosController {

    private final CarrosService service;

    @Autowired
    private HubCarroClient hubCarroClient;

    @Autowired
    private CarroReservaService carroReservaService;

    public CarrosController(CarrosService service) {
        this.service = service;
    }

    @PostMapping("/pesquisar")
    public List<PesquisaCarroResponseDTO> pesquisar(@RequestBody PesquisaCarroRequestDTO req) {
        List<PesquisaCarroResponseDTO> resultado = hubCarroClient.pesquisarDisponibilidade(req);
        return resultado;
    }

    @PostMapping("/selecionarCarro")
    public List<SelecionarCarroResponseDTO> selecionarCarro(@RequestBody SelecionarCarroRequestDTO req) {
        List<SelecionarCarroResponseDTO> resultado = hubCarroClient.selecionarCarro(req);
        return resultado;
    }

    @PostMapping("/reservar")
    public CarroReservaOperacaoResponseDTO efetuarReserva(@RequestBody CarroCompraModel req) {
        return carroReservaService.reservar(req);
    }

    @PostMapping("/consultarReserva")
    public CarroReservaOperacaoResponseDTO consultarReserva(@RequestBody ConsultarReservaCarroRequestDTO req) {
        return carroReservaService.consultarReserva(req);
    }

    @PostMapping("/cancelarReserva")
    public List<CancelarReservaCarroResponseDTO> cancelarReserva(@RequestBody CancelarCarroModel req) {
        return carroReservaService.cancelarReserva(req);
    }

    @PostMapping("/obterFormasPagamento")
    public List<FormasPagamentoCarroResponseDTO> obterFormasPagamento(@RequestBody FormasPagamentoCarroRequestDTO req) {
        List<FormasPagamentoCarroResponseDTO> resultado = hubCarroClient.obterFormasPagamento(req);
        return resultado;
    }

    @PostMapping("/emitir")
    public List<EmitirCarroResponseDTO> obterFormasPagamento(@RequestBody EmitirCarroRequestDTO req) {
        List<EmitirCarroResponseDTO> resultado = hubCarroClient.emitir(req);
        return resultado;
    }

    @GetMapping("/findAllReservas")
    public List<CarroReserva> findAllReservas() {
        return carroReservaService.findAllReservas();
    }
}
