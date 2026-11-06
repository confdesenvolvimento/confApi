package com.confApi.aereo;

import com.confApi.aereo.dto.*;
import com.confApi.aereo.dto.regrasAereas.AereoRegrasFamiliaRequest;
import com.confApi.aereo.dto.regrasAereas.RegrasAereasReservaResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v2/aereo")
public class AereoControllerV2 {
    private final AereoService service;
    private final AereoRegrasReservaService regrasReservaService;
    @Autowired
    private AereoClient aereoClient;

    public AereoControllerV2(AereoService service, AereoRegrasReservaService regrasReservaService) {
        this.service = service;
        this.regrasReservaService = regrasReservaService;
    }

    @PostMapping("/pesquisar")
    public List<PesquisaResponse> pesquisar(@RequestBody PesquisaRequestDTOV2 req) {
        List<PesquisaResponse> result = aereoClient.pesquisarDisponibilidadeV2(req);
        return result;
    }

    @PostMapping("/tarifar")
    public TarifarResponse tarifar(@RequestBody TarifarRequest req,
            @org.springframework.web.bind.annotation.RequestHeader(value="X-Usuario-Token", required=false) String usuario) {
        return "OPER".equals(req.getTipoTarifa()) ? aereoClient.tarifar(req, usuario) : aereoClient.tarifar(req);
    }
    public TarifarResponse tarifar(TarifarRequest req) { return aereoClient.tarifar(req); }

    @PostMapping("/reservar")
    public ReservarResponse tarifar(@RequestBody ReservarRequest req,
            @org.springframework.web.bind.annotation.RequestHeader(value="X-Usuario-Token", required=false) String usuario) {
        return "OPER".equals(req.getTipoTarifa()) ? aereoClient.reserva(req, usuario) : aereoClient.reserva(req);
    }
    public ReservarResponse tarifar(ReservarRequest req) { return aereoClient.reserva(req); }

    @PostMapping("/carregarReserva")
    public ConsultarLocalizadorResponse carregaReserva(@RequestBody ConsultarLocalizadorRequest req,
            @org.springframework.web.bind.annotation.RequestHeader(value="X-Usuario-Token", required=false) String usuario) {
        ConsultarLocalizadorResponse result = "OPER".equals(req.getTipoTarifa())
                ? aereoClient.carregarReserva(req, usuario) : aereoClient.carregarReserva(req);
        return regrasReservaService.enriquecer(result);
    }
    public ConsultarLocalizadorResponse carregaReserva(ConsultarLocalizadorRequest req) { return carregaReserva(req, null); }

    @PostMapping("/obterInformacoesDaFamilia")
    public RegrasAereasReservaResponse obterInformacoesDaFamilia(@RequestBody AereoRegrasFamiliaRequest req) {
        return regrasReservaService.consultarRegrasFamilia(req);
    }

    @PostMapping("/iniciarEmissao")
    public Map<String, Object> iniciarEmissao(@RequestBody ConsultarLocalizadorRequest req) {
        return aereoClient.iniciarEmissao(req);
    }

    @Autowired
    private AntifraudePortalHeaders antifraudePortalHeaders = new AntifraudePortalHeaders("", "");

    @PostMapping("/emitir")
    public EmitirResponse emitir(@RequestBody EmitirRequest req, javax.servlet.http.HttpServletRequest servletRequest) {
        return aereoClient.emitir(req, antifraudePortalHeaders.extract(servletRequest));
    }

    /** Compatibilidade dos consumidores Java; o Hub mantém o gate autoritativo. */
    public EmitirResponse emitir(EmitirRequest req) { return aereoClient.emitir(req); }

    @PostMapping("/cancelarReserva")
    public CancelarReservaResponse cancelarReserva(@RequestBody CancelarReservaRequest req,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Usuario-Token", required = false) String usuario) {
        return req != null && "OPER".equals(req.getTipoTarifa()) ? aereoClient.cancelarReserva(req, usuario) : aereoClient.cancelarReserva(req);
    }

    public CancelarReservaResponse cancelarReserva(CancelarReservaRequest req) { return cancelarReserva(req, null); }
    @PostMapping("/cancelarBilhete")
    public ConsultarEticketResponse cancelarBilhete(@RequestBody CancelarBilheteRequest req,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Usuario-Token", required = false) String usuario) {
        return req != null && "OPER".equals(req.getTipoTarifa()) ? aereoClient.cancelarBilhete(req, usuario) : aereoClient.cancelarBilhete(req);
    }

    public ConsultarEticketResponse cancelarBilhete(CancelarBilheteRequest req) { return cancelarBilhete(req, null); }

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
