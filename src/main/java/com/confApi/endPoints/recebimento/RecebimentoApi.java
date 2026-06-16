package com.confApi.endPoints.recebimento;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.recebimento.Recebimento;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class RecebimentoApi {

    private static final Logger LOG = Logger.getLogger(RecebimentoApi.class.getName());

    private final RestTemplate restTemplate;
    private final ConfAppService confAppService;

    public RecebimentoApi(RestTemplate restTemplate, ConfAppService confAppService) {
        this.restTemplate = restTemplate;
        this.confAppService = confAppService;
    }

    public List<Recebimento> findByReservaAereo(Integer codgReservaAereo) {
        if (codgReservaAereo == null) {
            return Collections.emptyList();
        }

        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                    .path("/recebimento")
                    .queryParam("codgReservaAereo.codgReservaAereo", codgReservaAereo)
                    .toUriString();

            ResponseEntity<List<Recebimento>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(defaultHeaders(token.getToken())),
                    new ParameterizedTypeReference<List<Recebimento>>() {
                    }
            );

            return response.getBody() == null ? Collections.emptyList() : response.getBody();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Erro ao consultar recebimentos da reserva aerea: " + codgReservaAereo, e);
            return Collections.emptyList();
        }
    }

    public void gravar(Recebimento recebimento) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                    .path("/recebimento/gravar")
                    .toUriString();

            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(recebimento, defaultHeaders(token.getToken())),
                    Void.class
            );
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao gravar recebimento da reserva Wooba.", e);
            throw e;
        }
    }

    public void atualizar(Integer codgRecebimento, Recebimento recebimento) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                    .path("/recebimento/{id}")
                    .buildAndExpand(codgRecebimento)
                    .toUriString();

            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(recebimento, defaultHeaders(token.getToken())),
                    Void.class
            );
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao atualizar recebimento da reserva Wooba. Id: " + codgRecebimento, e);
            throw e;
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
