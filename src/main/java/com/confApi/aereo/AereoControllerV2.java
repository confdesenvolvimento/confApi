package com.confApi.aereo;

import com.confApi.aereo.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v2/aereo")
public class AereoControllerV2 {
    private final AereoService service;
    @Autowired
    private AereoClient aereoClient;

    public AereoControllerV2(AereoService service) {
        this.service = service;
    }

    @PostMapping("/pesquisar")
    public List<PesquisaResponse> pesquisar(@RequestBody PesquisaRequestDTO req) {
        List<PesquisaResponse> result = aereoClient.pesquisarDisponibilidade(req);
        return result;
    }

    @PostMapping("/tarifar")
    public TarifarResponse tarifar(@RequestBody TarifarRequest req) {
        TarifarResponse result = aereoClient.tarifar(req);
        return result;
    }

    @PostMapping("/reservar")
    public ReservarResponse tarifar(@RequestBody ReservarRequest req) {
        ReservarResponse result = aereoClient.reserva(req);
        return result;
    }

    @PostMapping("/carregarReserva")
    public ConsultarLocalizadorResponse carregaReserva(@RequestBody ConsultarLocalizadorRequest req) {
        ConsultarLocalizadorResponse result = aereoClient.carregarReserva(req);
        return result;
    }

    @PostMapping("/emitir")
    public EmitirResponse emitir(@RequestBody EmitirRequest req) {
        EmitirResponse result = aereoClient.emitir(req);
        return result;
    }

    @PostMapping("/cancelarReserva")
    public CancelarReservaResponse cancelarReserva(@RequestBody CancelarReservaRequest req) {
        CancelarReservaResponse result = aereoClient.cancelarReserva(req);
        return result;
    }
    @PostMapping("/cancelarBilhete")
    public ConsultarEticketResponse cancelarBilhete(@RequestBody CancelarBilheteRequest req) {
        ConsultarEticketResponse result = aereoClient.cancelarBilhete(req);
        return result;
    }

    @PostMapping("/obterParcelas")
    public BuscarFormasFinanciamentoResponse obterParcelas(@RequestBody BuscarFormasFinanciamentoRequest req) {
        BuscarFormasFinanciamentoResponse result = aereoClient.recuperarFormasFinanciamento(req);
        return result;
    }

    @PostMapping("/mapaDeAssentos")
    public MapaAssentoResponse mapaDeAssentos(@RequestBody MapaAssentoRequest req) {
        MapaAssentoResponse result = aereoClient.buscarMapaAssentos(req);
        return result;
    }

    @PostMapping("/marcarAssento")
    public MarcarAssentoResponse marcarAssento(@RequestBody MarcarAssentoRequest req) {
        MarcarAssentoResponse result = aereoClient.marcarAssento(req);
        return result;
    }
    @PostMapping("/removerAssento")
    public RemoverAssentoResponse removerAssento(@RequestBody RemoverAssentoRequest req) {
        RemoverAssentoResponse result = aereoClient.removerAssento(req);
        return result;
    }

    @PostMapping("/tarifarPesquisa")
    public List<PesquisaResponse> tarifarPesquisa(@RequestBody TarifarPesquisaRequest req) {
        List<PesquisaResponse> result = aereoClient.tarifarPesquisa(req);
        return result;
    }
}
