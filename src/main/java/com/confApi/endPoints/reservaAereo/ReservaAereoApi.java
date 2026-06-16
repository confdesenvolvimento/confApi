package com.confApi.endPoints.reservaAereo;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.companhiaAerea.CompanhiaAerea;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.hub.aereo.ConsultarLocalizadorRequestHub;
import com.confApi.hub.aereo.ConsultarLocalizadorResponseHub;
import com.confApi.hub.aereo.PesquisaRequestAereoHub;
import com.confApi.hub.aereo.PesquisaResponseHub;
import com.confApi.util.TelegramErrorAlert;
import com.confApi.wooba.sales.WoobaAirlineCodeNormalizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ReservaAereoApi {

    private static final Logger LOG = Logger.getLogger(ReservaAereoApi.class.getName());

    @Autowired
    private ConfAppService confAppService;

    @Autowired(required = false)
    private TelegramErrorAlert telegramErrorAlert;

    private final RestTemplate restTemplate;

    public ReservaAereoApi(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ConsultarLocalizadorResponseHub reservaAereoConsultaLocalizadorHub(ConsultarLocalizadorRequestHub consultarLocalizadorRequestHub) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_HUB)
                    .path("api/aereo/consultar")
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<ConsultarLocalizadorRequestHub> entity =
                    new HttpEntity<>(consultarLocalizadorRequestHub, headers);

            ResponseEntity<ConsultarLocalizadorResponseHub> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ConsultarLocalizadorResponseHub>() {
                    }
            );

            return response.getBody();
        } catch (Exception e) {
            ConsultarLocalizadorResponseHub erro = new ConsultarLocalizadorResponseHub();
            erro.setException(e.getMessage());
            LOG.log(Level.SEVERE, "Erro ao consultar localizador no Hub.", e);
            alertarErro("Erro ao consultar localizador no Hub", e);
            return erro;
        }
    }

    public ReservaAereo reservaAereoConsultaLocalizadorDb(String localizador) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                    .path("/reservaAereo/localizador/" + localizador)
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<ReservaAereo> entity =
                    new HttpEntity<>(null, headers);

            ResponseEntity<ReservaAereo> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ReservaAereo>() {
                    }
            );

            return response.getBody();
        } catch (Exception e) {
            ReservaAereo erro = new ReservaAereo();
            LOG.log(Level.SEVERE, "Erro ao consultar reserva aerea por localizador no Manager: " + localizador, e);
            alertarErro("Erro ao consultar reserva aerea por localizador " + localizador, e);
            return erro;
        }
    }

    public List<ReservaAereo> reservaAereoConsultaLocalizadorDbLista(String localizador) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                    .path("/reservaAereo")
                    .queryParam("localizador", localizador)
                    .toUriString();

            ResponseEntity<List<ReservaAereo>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(defaultHeaders(token.getToken())),
                    new ParameterizedTypeReference<List<ReservaAereo>>() {
                    }
            );

            return response.getBody() == null ? Collections.emptyList() : response.getBody();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Erro ao consultar reservas aereas por localizador no Manager: " + localizador, e);
            alertarErro("Erro ao consultar lista de reservas aereas por localizador " + localizador, e);
            return Collections.emptyList();
        }
    }

    public ReservaAereo findByLocalizadorCompanhia(String localizador, CompanhiaAerea companhiaAerea) {
        if (localizador == null || localizador.trim().isEmpty()) {
            return null;
        }

        List<ReservaAereo> reservas = reservaAereoConsultaLocalizadorDbLista(localizador.trim());
        return reservas.stream()
                .filter(reserva -> localizador.trim().equalsIgnoreCase(defaultString(reserva.getLocalizador()).trim()))
                .filter(reserva -> mesmaCompanhia(reserva.getCodgCompanhiaAerea(), companhiaAerea))
                .findFirst()
                .orElse(null);
    }

    public ReservaAereo criar(ReservaAereo reservaAereo) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                    .path("/reservaAereo")
                    .toUriString();

            ResponseEntity<ReservaAereo> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(reservaAereo, defaultHeaders(token.getToken())),
                    ReservaAereo.class
            );

            return response.getBody();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao criar reserva aerea no Manager. Localizador: " + safeLocalizador(reservaAereo), e);
            alertarErro("Erro ao criar reserva aerea no Manager. Localizador " + safeLocalizador(reservaAereo), e);
            throw e;
        }
    }

    public void atualizar(Integer codgReservaAereo, ReservaAereo reservaAereo) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                    .path("/reservaAereo/{id}")
                    .buildAndExpand(codgReservaAereo)
                    .toUriString();

            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(reservaAereo, defaultHeaders(token.getToken())),
                    Void.class
            );
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao atualizar reserva aerea no Manager. Id: " + codgReservaAereo, e);
            alertarErro("Erro ao atualizar reserva aerea no Manager. Id " + codgReservaAereo, e);
            throw e;
        }
    }

    public void cancelar(Integer codgReservaAereo, Date dataCancelamento, String motivo, Integer codgUsuarioCancelamento) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                    .path("/reservaAereo/cancelar/{id}")
                    .buildAndExpand(codgReservaAereo)
                    .toUriString();

            Map<String, Object> payload = new HashMap<>();
            payload.put("codgReservaAereo", codgReservaAereo);
            payload.put("dataCancelamento", dataCancelamento);
            payload.put("descMotivoCancelamento", motivo);
            payload.put("codgUsuarioCancelamento", codgUsuarioCancelamento);

            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(payload, defaultHeaders(token.getToken())),
                    Void.class
            );
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao cancelar reserva aerea no Manager. Id: " + codgReservaAereo, e);
            alertarErro("Erro ao cancelar reserva aerea no Manager. Id " + codgReservaAereo, e);
            throw e;
        }
    }

    public void atualizarStatus(Integer codgReservaAereo, Integer status) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                    .path("/reservaAereo/atualizarReserva/{id}")
                    .buildAndExpand(codgReservaAereo)
                    .toUriString();

            Map<String, Object> payload = new HashMap<>();
            payload.put("codgReservaAereo", codgReservaAereo);
            payload.put("reservaStatus", status);

            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(payload, defaultHeaders(token.getToken())),
                    Void.class
            );
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao atualizar status da reserva aerea no Manager. Id: " + codgReservaAereo, e);
            alertarErro("Erro ao atualizar status da reserva aerea no Manager. Id " + codgReservaAereo, e);
            throw e;
        }
    }

    private void alertarErro(String mensagem, Exception e) {
        if (telegramErrorAlert != null) {
            telegramErrorAlert.enviar(this, mensagem, e);
        }
    }

    private boolean mesmaCompanhia(CompanhiaAerea left, CompanhiaAerea right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.getIataCia() != null && right.getIataCia() != null) {
            return WoobaAirlineCodeNormalizer.sameIata(left.getIataCia(), right.getIataCia());
        }
        if (left.getCodgCompanhiaAerea() != null && right.getCodgCompanhiaAerea() != null) {
            return left.getCodgCompanhiaAerea().equals(right.getCodgCompanhiaAerea());
        }
        return false;
    }

    private String safeLocalizador(ReservaAereo reservaAereo) {
        return reservaAereo == null ? null : reservaAereo.getLocalizador();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private HttpHeaders defaultHeaders(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(bearerToken);
        return headers;
    }
}

