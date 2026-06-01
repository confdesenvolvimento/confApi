package com.confApi.aereo;

import com.confApi.aereo.dto.*;
import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.hub.seguro.HubSeguroClient;
import com.confApi.seguros.dto.PlanoSeguroDTO;
import com.confApi.seguros.dto.SeguroViagemPesquisaDTO;
import com.confApi.util.JsonLogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class AereoClient {

    private final RestTemplate restTemplate;

    private static final Logger LOG = Logger.getLogger(AereoClient.class.getName());
    @Autowired
    private ConfAppService confAppService;


    public AereoClient(RestTemplate hubRestTemplate) {
        this.restTemplate = hubRestTemplate;
    }

    public BuscarFormasFinanciamentoResponse recuperarFormasFinanciamentoToken(BuscarFormasFinanciamentoRequest request) {

        BuscarFormasFinanciamentoResponse buscarFormasFinanciamentoResponse =
                new BuscarFormasFinanciamentoResponse();

        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<BuscarFormasFinanciamentoRequest> entity =
                    new HttpEntity<>(request, headers);

            JsonLogUtil.logRequest("Aéreo - Recuperar Formas de Financiamento Token", request);

            ResponseEntity<BuscarFormasFinanciamentoResponse> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_HUB + "/api/aereo/recuperarformasdefinanciamentoToken",
                    HttpMethod.POST,
                    entity,
                    BuscarFormasFinanciamentoResponse.class
            );

            JsonLogUtil.logResponse("Aéreo - Recuperar Formas de Financiamento Token", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            LOG.log(
                    Level.WARNING,
                    "recuperar formas de financiamento com token retornou status {0} sem corpo válido",
                    response.getStatusCode()
            );

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao recuperar formas de financiamento com token no HUB", e);
        }

        return buscarFormasFinanciamentoResponse;
    }
    public List<PesquisaResponse> tarifarPesquisa(TarifarPesquisaRequest request) {

        List<PesquisaResponse> pesquisaResponseList = new ArrayList<>();

        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<TarifarPesquisaRequest> entity =
                    new HttpEntity<>(request, headers);

            JsonLogUtil.logRequest("Aéreo - Tarifar Pesquisa", request);

            ResponseEntity<List<PesquisaResponse>> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_HUB + "/api/aereo/tarifarpesquisa",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<PesquisaResponse>>() {}
            );

            JsonLogUtil.logResponse("Aéreo - Tarifar Pesquisa", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            LOG.log(
                    Level.WARNING,
                    "tarifar pesquisa retornou status {0} sem corpo válido",
                    response.getStatusCode()
            );

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao consumir tarifar pesquisa aérea no HUB", e);
        }

        return pesquisaResponseList;
    }

    public RemoverAssentoResponse removerAssento(RemoverAssentoRequest request) {

        RemoverAssentoResponse removerAssentoResponse = new RemoverAssentoResponse();

        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<RemoverAssentoRequest> entity =
                    new HttpEntity<>(request, headers);

            JsonLogUtil.logRequest("Aéreo - Remover Assento", request);

            ResponseEntity<RemoverAssentoResponse> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_HUB + "/api/aereo/removerassento",
                    HttpMethod.POST,
                    entity,
                    RemoverAssentoResponse.class
            );

            JsonLogUtil.logResponse("Aéreo - Remover Assento", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            LOG.log(
                    Level.WARNING,
                    "remover assento retornou status {0} sem corpo válido",
                    response.getStatusCode()
            );

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao remover assento no HUB", e);
        }

        return removerAssentoResponse;
    }

    public MarcarAssentoResponse marcarAssento(MarcarAssentoRequest request) {

        MarcarAssentoResponse marcarAssentoResponse = new MarcarAssentoResponse();

        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<MarcarAssentoRequest> entity =
                    new HttpEntity<>(request, headers);

            JsonLogUtil.logRequest("Aéreo - Marcar Assento", request);

            ResponseEntity<MarcarAssentoResponse> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_HUB + "/api/aereo/marcarassento",
                    HttpMethod.POST,
                    entity,
                    MarcarAssentoResponse.class
            );

            JsonLogUtil.logResponse("Aéreo - Marcar Assento", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            LOG.log(
                    Level.WARNING,
                    "marcar assento retornou status {0} sem corpo válido",
                    response.getStatusCode()
            );

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao marcar assento no HUB", e);
        }

        return marcarAssentoResponse;
    }
    public MapaAssentoResponse buscarMapaAssentos(MapaAssentoRequest request) {
        MapaAssentoResponse mapaAssentoResponse = new MapaAssentoResponse();
        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<MapaAssentoRequest> entity =
                    new HttpEntity<>(request, headers);

            JsonLogUtil.logRequest("Aéreo - Buscar Mapa de Assentos", request);

            ResponseEntity<MapaAssentoResponse> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_HUB + "/api/aereo/mapadeassentos",
                    HttpMethod.POST,
                    entity,
                    MapaAssentoResponse.class
            );

            JsonLogUtil.logResponse("Aéreo - Buscar Mapa de Assentos", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            LOG.log(
                    Level.WARNING,
                    "buscar mapa de assentos retornou status {0} sem corpo válido",
                    response.getStatusCode()
            );

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao buscar mapa de assentos no HUB", e);
        }

        return mapaAssentoResponse;
    }
    public BuscarFormasFinanciamentoResponse recuperarFormasFinanciamento(BuscarFormasFinanciamentoRequest request) {

        BuscarFormasFinanciamentoResponse buscarFormasFinanciamentoResponse =
                new BuscarFormasFinanciamentoResponse();

        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<BuscarFormasFinanciamentoRequest> entity =
                    new HttpEntity<>(request, headers);

            JsonLogUtil.logRequest("Aéreo - Recuperar Formas de Financiamento", request);

            ResponseEntity<BuscarFormasFinanciamentoResponse> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_HUB + "/api/aereo/recuperarformasdefinanciamento",
                    HttpMethod.POST,
                    entity,
                    BuscarFormasFinanciamentoResponse.class
            );

            JsonLogUtil.logResponse("Aéreo - Recuperar Formas de Financiamento", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            LOG.log(
                    Level.WARNING,
                    "recuperar formas de financiamento retornou status {0} sem corpo válido",
                    response.getStatusCode()
            );

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao recuperar formas de financiamento no HUB", e);
        }

        return buscarFormasFinanciamentoResponse;
    }

    public ConsultarEticketResponse cancelarBilhete(CancelarBilheteRequest request) {

        ConsultarEticketResponse cancelarBilheteResponse = new ConsultarEticketResponse();

        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<CancelarBilheteRequest> entity = new HttpEntity<>(request, headers);

            JsonLogUtil.logRequest("Aéreo - Cancelar Bilhete", request);

            ResponseEntity<ConsultarEticketResponse> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_HUB + "/api/aereo/cancelarbilhete",
                    HttpMethod.POST,
                    entity,
                    ConsultarEticketResponse.class
            );

            JsonLogUtil.logResponse("Aéreo - Cancelar Bilhete", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            LOG.log(
                    Level.WARNING,
                    "cancelar bilhete retornou status {0} sem corpo válido",
                    response.getStatusCode()
            );

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao cancelar bilhete aéreo no HUB", e);
        }

        return cancelarBilheteResponse;
    }

    public CancelarReservaResponse cancelarReserva(CancelarReservaRequest request) {

        CancelarReservaResponse cancelarReservaResponse = new CancelarReservaResponse();

        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<CancelarReservaRequest> entity = new HttpEntity<>(request, headers);

            JsonLogUtil.logRequest("Aéreo - Cancelar Reserva", request);

            ResponseEntity<CancelarReservaResponse> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_HUB + "/api/aereo/cancelar",
                    HttpMethod.POST,
                    entity,
                    CancelarReservaResponse.class
            );

            JsonLogUtil.logResponse("Aéreo - Cancelar Reserva", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            LOG.log(
                    Level.WARNING,
                    "cancelar reserva retornou status {0} sem corpo válido",
                    response.getStatusCode()
            );

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao cancelar reserva aérea no HUB", e);
        }

        return cancelarReservaResponse;
    }
    public EmitirResponse emitir(EmitirRequest request) {

        EmitirResponse emitirResponse = new EmitirResponse();

        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<EmitirRequest> entity = new HttpEntity<>(request, headers);

            JsonLogUtil.logRequest("Aéreo - Emitir", request);

            ResponseEntity<EmitirResponse> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_HUB + "/api/aereo/emitir",
                    HttpMethod.POST,
                    entity,
                    EmitirResponse.class
            );

            JsonLogUtil.logResponse("Aéreo - Emitir", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            LOG.log(
                    Level.WARNING,
                    "emitir retornou status {0} sem corpo válido",
                    response.getStatusCode()
            );

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao emitir aéreo no HUB", e);
        }

        return emitirResponse;
    }

    public ConsultarLocalizadorResponse carregarReserva(ConsultarLocalizadorRequest consultarRequest) {

        ConsultarLocalizadorResponse consultarResponse = new ConsultarLocalizadorResponse();

        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<ConsultarLocalizadorRequest> entity = new HttpEntity<>(consultarRequest, headers);

            JsonLogUtil.logRequest("Aéreo - Consultar Localizador", consultarRequest);

            ResponseEntity<ConsultarLocalizadorResponse> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_HUB + "/api/aereo/consultar",
                    HttpMethod.POST,
                    entity,
                    ConsultarLocalizadorResponse.class
            );

            JsonLogUtil.logResponse("Aéreo - Consultar Localizador", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            LOG.log(
                    Level.WARNING,
                    "consultar localizador retornou status {0} sem corpo válido",
                    response.getStatusCode()
            );

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao consultar localizador aéreo no HUB", e);
        }

        return consultarResponse;
    }
    public ReservarResponse reserva(ReservarRequest reservarRequest) {

        ReservarResponse reservarResponse = new ReservarResponse();

        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<ReservarRequest> entity = new HttpEntity<>(reservarRequest, headers);

            JsonLogUtil.logRequest("Aéreo - Reservar", reservarRequest);

            ResponseEntity<ReservarResponse> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_HUB + "/api/aereo/reservar",
                    HttpMethod.POST,
                    entity,
                    ReservarResponse.class
            );

            JsonLogUtil.logResponse("Aéreo - Reservar", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            LOG.log(
                    Level.WARNING,
                    "reservar retornou status {0} sem corpo válido",
                    response.getStatusCode()
            );

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao reservar aéreo no HUB", e);
        }

        return reservarResponse;
    }

    public TarifarResponse tarifar(TarifarRequest tarifarRequest) {

        TarifarResponse tarifarResponse = new TarifarResponse();

        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<TarifarRequest> entity = new HttpEntity<>(tarifarRequest, headers);
            JsonLogUtil.logRequest("Aéreo - Tarifar", tarifarRequest);
            ResponseEntity<TarifarResponse> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_HUB + "/api/aereo/tarifar",
                    HttpMethod.POST,
                    entity,
                    TarifarResponse.class
            );
            JsonLogUtil.logResponse("Aéreo - Tarifar", response.getBody());
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {

                return response.getBody();
            }

            LOG.log(
                    Level.WARNING,
                    "tarifar retornou status {0} sem corpo válido",
                    response.getStatusCode()
            );

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao tarifar reserva aérea no HUB", e);
        }

        return tarifarResponse;
    }

    public List<PesquisaResponse> pesquisarDisponibilidade(PesquisaRequestDTO pesquisaRequestDTO) {
        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<PesquisaRequestDTO> entity = new HttpEntity<>(pesquisaRequestDTO, headers);

            ResponseEntity<List<PesquisaResponse>> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_HUB + "/api/aereo/pesquisa",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<PesquisaResponse>>() {
                    }
            );

            List<PesquisaResponse> body = response.getBody();

            if (response.getStatusCode().is2xxSuccessful() && body != null) {
                return body;
            }

            LOG.log(Level.WARNING,
                    "pesquisarDisponibilidade retornou status {0} sem corpo válido",
                    response.getStatusCode());

            return Collections.emptyList();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao pesquisar disponibilidade aérea no HUB", e);
            return Collections.emptyList();
        }
    }


    private HttpHeaders defaultHeaders(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(bearerToken);
        return headers;
    }
}
