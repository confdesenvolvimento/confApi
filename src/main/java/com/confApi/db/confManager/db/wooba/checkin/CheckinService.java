package com.confApi.db.confManager.db.wooba.checkin;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.confApp.dto.SandBoxResp;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.db.wooba.checkin.dto.Checkin;
import com.confApi.db.confManager.db.wooba.checkin.dto.Checkin72Horas;
import com.confApi.db.confManager.db.wooba.checkin.dto.CheckinRQ;
import com.confApi.hub.limites.LimitesService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
@Service
public class CheckinService {
    private static final Logger LOG = Logger.getLogger(CheckinService.class.getName());

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    private static final String API_ACTION_VOO_DIA = "reservaTrecho/voosDoDia";
    private static final String API_ACTION_CHECKIN = "reservaTrecho/checkin";

    @Autowired
    private ConfAppService confAppService;

    public CheckinService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }

    /** Retorna voos do dia para check-in (lista de Checkin). */
    public List<Checkin> findVoosDoDia(CheckinRQ rq) {
        return postForList(API_ACTION_VOO_DIA, rq, new TypeReference<List<Checkin>>() {});
    }

    /** Retorna janelas de check-in (72h) (lista de Checkin72Horas). */
    public List<Checkin72Horas> findCheckin72Horas(CheckinRQ rq) {
        return postForList(API_ACTION_CHECKIN, rq, new TypeReference<List<Checkin72Horas>>() {});
    }

    /* ============================ PRIVADOS ============================ */

    private <T> List<T> postForList(String action, Object body, TypeReference<List<T>> typeRef) {
        String url = buildUrl(action);
        LOG.info(() -> "[CheckinService] URL chamada: " + url);

        HttpEntity<String> entity;
        try {
            String jsonBody = mapper.writeValueAsString(body);
            HttpHeaders headers = defaultHeaders(); // <-- inclui o token aqui
            entity = new HttpEntity<>(jsonBody, headers);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "[CheckinService] Falha ao serializar request body", e);
            return Collections.emptyList();
        }

        String responseBody;
        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                LOG.log(Level.WARNING, "[CheckinService] HTTP {0} em {1}", new Object[]{resp.getStatusCode(), url});
                return Collections.emptyList();
            }
            responseBody = Optional.ofNullable(resp.getBody()).orElse("");
            if (responseBody.isBlank()) {
                LOG.log(Level.WARNING, "[CheckinService] Resposta vazia em {0}", url);
                return Collections.emptyList();
            }
        } catch (RestClientException ex) {
            LOG.log(Level.SEVERE, "[CheckinService] Erro HTTP em " + url, ex);
            return Collections.emptyList();
        }

        try {
            List<T> list = mapper.readValue(responseBody, typeRef);
            if (list == null || list.isEmpty()) {
                LOG.log(Level.INFO, "[CheckinService] Lista vazia retornada por {0}", url);
                return Collections.emptyList();
            }
            return list;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "[CheckinService] Falha ao parsear resposta de " + url, e);
            return Collections.emptyList();
        }
    }

    /** Monta os headers padrão com Bearer Token. */
    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();
            if (token != null && token.getToken() != null && !token.getToken().isBlank()) {
                headers.setBearerAuth(token.getToken());
                LOG.info("[CheckinService] Token Bearer adicionado com sucesso.");
            } else {
                LOG.warning("[CheckinService] Token nulo ou inválido retornado por confAppService.token()");
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "[CheckinService] Falha ao obter token do ConfAppService", e);
        }

        return headers;
    }

    /** Monta URL completa para chamada da API. */
    private String buildUrl(String action) {
        String base = null;
        try {
            base = UrlConfig.URL_CONFIANCA_MANAGER;
        } catch (Exception ignore) { /* mantém null */ }

        if (base == null || base.isBlank()) {
            base = UrlConfig.URL_CONFIANCA_MANAGER;
        }

        if (!base.endsWith("/")) base += "/";
        return base + action;
    }

}
