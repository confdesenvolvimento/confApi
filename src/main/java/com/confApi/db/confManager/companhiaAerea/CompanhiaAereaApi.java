package com.confApi.db.confManager.companhiaAerea;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
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
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class CompanhiaAereaApi {

    private static final Logger LOG = Logger.getLogger(CompanhiaAereaApi.class.getName());

    private final RestTemplate restTemplate;
    private final ConfAppService confAppService;

    public CompanhiaAereaApi(RestTemplate restTemplate, ConfAppService confAppService) {
        this.restTemplate = restTemplate;
        this.confAppService = confAppService;
    }

    public CompanhiaAerea findByIataCia(String iataCia) {
        if (iataCia == null || iataCia.trim().isEmpty()) {
            return null;
        }

        String normalizedIata = iataCia.trim().toUpperCase(Locale.ROOT);

        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                    .path("/companhiaAereo")
                    .queryParam("iataCia", normalizedIata)
                    .toUriString();

            ResponseEntity<List<CompanhiaAerea>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(defaultHeaders(token.getToken())),
                    new ParameterizedTypeReference<List<CompanhiaAerea>>() {
                    }
            );

            List<CompanhiaAerea> companhias = response.getBody();
            if (companhias == null || companhias.isEmpty()) {
                return null;
            }

            return companhias.stream()
                    .filter(cia -> normalizedIata.equalsIgnoreCase(cia.getIataCia()))
                    .findFirst()
                    .orElse(companhias.get(0));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Nao foi possivel consultar companhia aerea por IATA: " + normalizedIata, e);
            return null;
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
