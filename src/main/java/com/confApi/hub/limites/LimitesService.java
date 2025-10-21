package com.confApi.hub.limites;

import com.confApi.confApp.ConfAppService;
import com.confApi.confApp.dto.SandBoxResp;
import com.confApi.config.UrlConfig;
import com.confApi.hub.limites.dto.StatusResponse;
import com.confApi.hub.limites.dto.Disponibilidade;
import com.confApi.hub.limites.dto.LimiteCreditoRQ;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class LimiteCreditoService {

    private static final Logger LOG = Logger.getLogger(LimiteCreditoService.class.getName());

    private final RestTemplate restTemplate;

    @Autowired
    private ConfAppService confAppService;

    // Mantém os mesmos trechos da sua classe original
    private static final String API_ACTION = "api/limite";

    public LimiteCreditoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * POST: {base}/api/limite
     */
    public Disponibilidade consultaLimiteApi(LimiteCreditoRQ limiteCreditoRQ) {
        try {
            // 1) Token
            SandBoxResp token = confAppService.token();

            // 2) URL
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_HUB)
                    .path(API_ACTION)
                    .toUriString();

            // 3) Headers + body
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<LimiteCreditoRQ> entity = new HttpEntity<>(limiteCreditoRQ, headers);

            // 4) Chamada
            ResponseEntity<Disponibilidade> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Disponibilidade.class
            );

            Disponibilidade body = response.getBody();

            // 5) Regras mínimas de validação (similar ao que você fazia)
            if (response.getStatusCode().is2xxSuccessful() && body != null) {
                // se precisar manter a checagem de lista vazia:
                if (body.getLimiteCredito() == null) {
                    // evitar NPE para quem consome
                    body.setLimiteCredito(Collections.emptyList());
                }
                return body;
            }

            LOG.log(Level.WARNING, "consultaLimiteApi retornou status {0} sem corpo válido",
                    response.getStatusCode());
            return emptyDisponibilidade();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao consultar limite de crédito", e);
            return emptyDisponibilidade();
        }
    }

    /**
     * GET: {base}/api/limite/checkUrlLimite/{idErp}
     */
    public StatusResponse checkLimiteApi(LimiteCreditoRQ limiteCreditoRQ) {
        try {
            // 1) Token
            SandBoxResp token = confAppService.token();

            // 2) URL com path variable
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_HUB)
                    .path(API_ACTION)
                    .path("/checkUrlLimite/{idErp}")
                    .buildAndExpand(limiteCreditoRQ.getIdErp())
                    .toUriString();

            // 3) Headers (GET sem body)
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // 4) Chamada
            ResponseEntity<StatusResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<StatusResponse>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            LOG.log(Level.WARNING, "checkLimiteApi retornou status {0} sem corpo válido",
                    response.getStatusCode());
            return defaultStatusFalha("Resposta inválida do serviço de limites.");

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao checar limite (checkUrlLimite)", e);
            return defaultStatusFalha("Erro ao checar limite (exceção).");
        }
    }

    // ==========================
    // Helpers
    // ==========================

    private HttpHeaders defaultHeaders(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(bearerToken);
        return headers;
    }

    private Disponibilidade emptyDisponibilidade() {
        Disponibilidade d = new Disponibilidade();
        d.setLimiteCredito(Collections.emptyList());
        return d;
    }

    private StatusResponse defaultStatusFalha(String msg) {
        StatusResponse s = new StatusResponse();
        s.setStatusCode(1);
        s.setMessage(msg);
        return s;
    }
}