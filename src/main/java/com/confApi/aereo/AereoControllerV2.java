package com.confApi.aereo;

import com.confApi.aereo.dto.*;
import com.confApi.aereo.dto.regrasAereas.AereoRegrasFamiliaRequest;
import com.confApi.aereo.dto.regrasAereas.RegrasAereasReservaResponse;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.hub.aereo.ReservaAereoModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v2/aereo")
public class AereoControllerV2 {

    @Autowired
    private AereoRegrasReservaService regrasReservaService;

    @Autowired
    private AereoClient aereoClient;

    @Autowired
    private AereoClientV2 aereoClientV2;

    @PostMapping("/pesquisar")
    public PesquisaResponse pesquisar(@RequestBody PesquisaRequestDTOV2 req) {
        return aereoClientV2.pesquisarDisponibilidade(req);
    }

    @PostMapping("/tarifar")
    public PreReserva tarifar(@RequestBody PreReserva req) {
        return aereoClientV2.tarifar(req);
    }

    @PostMapping("/reservar")
    public ReservarResponse reservar(@RequestBody PreReserva req) {
        ReservarResponse result = aereoClientV2.reserva(req);
        return result;
    }

    @PostMapping("/carregarReserva")
    public ConsultarLocalizadorResponse carregaReserva(@RequestBody ConsultarLocalizadorRequest req) {
        ConsultarLocalizadorResponse result = aereoClient.carregarReserva(req);
        return regrasReservaService.enriquecer(result);
    }

    @PostMapping("/obterInformacoesDaFamilia")
    public RegrasAereasReservaResponse obterInformacoesDaFamilia(@RequestBody AereoRegrasFamiliaRequest req) {
        return regrasReservaService.consultarRegrasFamilia(req);
    }

    @PostMapping("/carregarReservaModel")
        public ReservaAereoModel carregaReservaModel(@RequestBody ReservaAereo req) {
        ReservaAereoModel result = aereoClientV2.carregarReservaAerea(req);
        return result;
    }

    @PostMapping("/emitir/{isLink}")
    public ReservaAereoModel emitir(@PathVariable Boolean isLink, @RequestBody ReservaAereoModel req) {
        return aereoClientV2.emitir(req, isLink);
    }

    @PostMapping("/cancelarReserva")
    public Boolean cancelarReserva(@RequestBody ReservaAereoModel reservaAerea) {
        return aereoClientV2.cancelarReserva(reservaAerea);
    }

    @PostMapping("/cancelarBilhete")
    public ConsultarEticketResponse cancelarBilhete(@RequestBody ReservaAereoModel req) {
        ConsultarEticketResponse result = aereoClientV2.cancelarBilhete(req);
        return result;
    }

    @PostMapping("/obterParcelas")
    public BuscarFormasFinanciamentoResponse obterParcelas(@RequestBody BuscarFormasFinanciamentoRequest req) {
        BuscarFormasFinanciamentoResponse result = aereoClientV2.recuperarFormasFinanciamento(req);
        return result;
    }

    @PostMapping("/mapaDeAssentos")
    public MapaAssentoResponse mapaDeAssentos(@RequestBody MapaAssentoRequest req) {
        MapaAssentoResponse result = aereoClientV2.buscarMapaAssentos(req);
        return result;
    }

    @PostMapping("/marcarAssento")
    public MarcarAssentoResponse marcarAssento(@RequestBody MarcarAssentoRequest req) {
        MarcarAssentoResponse result = aereoClientV2.marcarAssento(req);
        return result;
    }
    @PostMapping("/removerAssento")
    public RemoverAssentoResponse removerAssento(@RequestBody RemoverAssentoRequest req) {
        RemoverAssentoResponse result = aereoClientV2.removerAssento(req);
        return result;
    }

    @PostMapping("/tarifarPesquisa")
    public List<PesquisaResponse> tarifarPesquisa(@RequestBody TarifarPesquisaRequest req) {
        List<PesquisaResponse> result = aereoClientV2.tarifarPesquisa(req);
        return result;
    }
}
