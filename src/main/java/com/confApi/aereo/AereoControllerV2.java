package com.confApi.aereo;

import com.confApi.aereo.dto.*;
import com.confApi.aereo.dto.regrasAereas.AereoRegrasFamiliaRequest;
import com.confApi.aereo.dto.regrasAereas.RegrasAereasReservaResponse;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.hub.aereo.ReservaAereoModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v2/aereo")
public class AereoControllerV2 {

    public AereoControllerV2() {}

    public AereoControllerV2(AereoService service, AereoRegrasReservaService regrasReservaService) {
        this.regrasReservaService = regrasReservaService;
    }

    @Autowired
    private AereoRegrasReservaService regrasReservaService;

    @Autowired
    private AereoClient aereoClient;

    @Autowired
    private AereoClientV2 aereoClientV2;

    @Autowired
    private AereoConsultaService consultaService;

    @Autowired
    private ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @PostMapping("/pesquisar")
    public PesquisaResponse pesquisar(@RequestBody PesquisaRequestDTOV2 req) {
        return aereoClientV2.pesquisarDisponibilidade(req);
    }

    @PostMapping("/pesquisarApp")
    public List<PesquisaResponse> pesquisarApp(@RequestBody PesquisaRequestDTOV2 req) {
        List<PesquisaResponse> result = aereoClient.pesquisarDisponibilidadeV2(req);
        return result;
    }

    @PostMapping("/tarifar")
    public Object tarifarCompativel(@RequestBody JsonNode req,
            @RequestHeader(value = "X-Usuario-Token", required = false) String usuario) {
        if (!tarifaOperadora(req) && req.has("trechos")) {
            return tarifar(converter(req, PreReserva.class));
        }
        return tarifar(converter(req, TarifarRequest.class), usuario);
    }

    public TarifarResponse tarifar(TarifarRequest req, String usuario) {
        return "OPER".equals(req.getTipoTarifa()) ? aereoClient.tarifar(req, usuario) : aereoClient.tarifar(req);
    }

    public PreReserva tarifar(PreReserva req) {
        return aereoClientV2.tarifar(req);
    }
    public TarifarResponse tarifar(TarifarRequest req) { return aereoClient.tarifar(req); }

    @PostMapping("/reservar")
    public ReservarResponse reservarCompativel(@RequestBody JsonNode req,
            @RequestHeader(value = "X-Usuario-Token", required = false) String usuario) {
        if (!tarifaOperadora(req) && req.has("trechos")) {
            return reservar(converter(req, PreReserva.class));
        }
        return tarifar(converter(req, ReservarRequest.class), usuario);
    }

    public ReservarResponse tarifar(ReservarRequest req, String usuario) {
        return "OPER".equals(req.getTipoTarifa()) ? aereoClient.reserva(req, usuario) : aereoClient.reserva(req);
    }

    public ReservarResponse reservar(PreReserva req) {
        ReservarResponse result = aereoClientV2.reserva(req);
        return result;
    }
    public ReservarResponse tarifar(ReservarRequest req) { return aereoClient.reserva(req); }

    @PostMapping("/carregarReserva")
    public ConsultarLocalizadorResponse carregaReserva(@RequestBody ConsultarLocalizadorRequest req,
            @org.springframework.web.bind.annotation.RequestHeader(value="X-Usuario-Token", required=false) String usuario) {
        ConsultarLocalizadorResponse result = "OPER".equals(req.getTipoTarifa())
                ? aereoClient.carregarReserva(req, usuario) : consultaService.carregarReserva(req);
        return regrasReservaService.enriquecer(result);
    }
    public ConsultarLocalizadorResponse carregaReserva(ConsultarLocalizadorRequest req) { return carregaReserva(req, null); }

    @PostMapping("/obterInformacoesDaFamilia")
    public RegrasAereasReservaResponse obterInformacoesDaFamilia(@RequestBody AereoRegrasFamiliaRequest req) {
        return regrasReservaService.consultarRegrasFamilia(req);
    }

    @PostMapping("/carregarReservaModel")
    public ReservaAereoModel carregaReservaModel(@RequestBody ReservaAereo req) {
        ReservaAereoModel result = aereoClientV2.carregarReservaAerea(req);
        return result;
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

    @PostMapping("/emitir/{isLink}")
    public ReservaAereoModel emitir(@PathVariable Boolean isLink, @RequestBody ReservaAereoModel req,
                                    javax.servlet.http.HttpServletRequest servletRequest) {
        return aereoClientV2.emitir(req, isLink, antifraudePortalHeaders.extract(servletRequest));
    }

    /** Compatibilidade dos consumidores Java; o Hub mantém o gate autoritativo. */
    public EmitirResponse emitir(EmitirRequest req) { return aereoClient.emitir(req); }

    @PostMapping("/cancelarReserva")
    public Object cancelarReservaCompativel(@RequestBody JsonNode req,
            @RequestHeader(value = "X-Usuario-Token", required = false) String usuario) {
        if (!tarifaOperadora(req) && reservaModel(req)) {
            return cancelarReserva(converter(req, ReservaAereoModel.class));
        }
        return cancelarReserva(converter(req, CancelarReservaRequest.class), usuario);
    }

    @PostMapping("/cancelarReservaRequest")
    public CancelarReservaResponse cancelarReserva(@RequestBody CancelarReservaRequest req,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Usuario-Token", required = false) String usuario) {
        return req != null && "OPER".equals(req.getTipoTarifa()) ? aereoClient.cancelarReserva(req, usuario) : aereoClient.cancelarReserva(req);
    }

    public CancelarReservaResponse cancelarReserva(CancelarReservaRequest req) { return cancelarReserva(req, null); }

    @PostMapping("/cancelarBilhete")
    public ConsultarEticketResponse cancelarBilheteCompativel(@RequestBody JsonNode req,
            @RequestHeader(value = "X-Usuario-Token", required = false) String usuario) {
        if (!tarifaOperadora(req) && reservaModel(req)) {
            return cancelarBilhete(converter(req, ReservaAereoModel.class));
        }
        return cancelarBilhete(converter(req, CancelarBilheteRequest.class), usuario);
    }

    public ConsultarEticketResponse cancelarBilhete(CancelarBilheteRequest req, String usuario) {
        return req != null && "OPER".equals(req.getTipoTarifa()) ? aereoClient.cancelarBilhete(req, usuario) : aereoClient.cancelarBilhete(req);
    }

    public Boolean cancelarReserva(ReservaAereoModel reservaAerea) {
        return aereoClientV2.cancelarReserva(reservaAerea);
    }

    public ConsultarEticketResponse cancelarBilhete(ReservaAereoModel req) {
        ConsultarEticketResponse result = aereoClientV2.cancelarBilhete(req);
        return result;
    }

    public ConsultarEticketResponse cancelarBilhete(CancelarBilheteRequest req) { return cancelarBilhete(req, null); }

    // As mesmas rotas recebem os DTOs do Hub e os modelos completos usados pelo front.
    private boolean tarifaOperadora(JsonNode req) {
        String tipo = req.path("tipoTarifa").asText("");
        if (!tipo.isBlank() && !"CONS".equals(tipo) && !"OPER".equals(tipo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de tarifa inválido.");
        }
        return "OPER".equals(tipo);
    }

    private boolean reservaModel(JsonNode req) {
        return req.has("codgReservaAereoDB") || req.has("passageiros");
    }

    private <T> T converter(JsonNode req, Class<T> tipo) {
        if (!req.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um objeto JSON.");
        }
        try {
            return objectMapper.convertValue(req, tipo);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requisição aérea inválida.", ex);
        }
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
