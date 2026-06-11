package com.confApi.carros;

import com.confApi.carros.dto.*;
import com.confApi.hub.carro.HubCarroClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/carro")
public class CarrosController {

    private final CarrosService service;

    @Autowired
    private HubCarroClient hubCarroClient;

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
    public List<EmitirCarroResponseDTO> efetuarReserva(@RequestBody ReservarCarroRequestDTO req) {
        return hubCarroClient.reservar(req);
    }

    @PostMapping("/consultarReserva")
    public List<ReservarCarroResponseDTO> consultarReserva(@RequestBody ConsultarReservaCarroRequestDTO req) {
        List<ReservarCarroResponseDTO> resultado = hubCarroClient.consultarReserva(req);
        return resultado;
    }

    @PostMapping("/cancelarReserva")
    public List<CancelarReservaCarroResponseDTO> cancelarReserva(@RequestBody CancelarReservaCarroRequestDTO req) {
        List<CancelarReservaCarroResponseDTO> resultado = hubCarroClient.cancelarReserva(req);
        return resultado;
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
}
