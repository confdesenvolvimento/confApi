package com.confApi.endPoints.clube.quemParticipa;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import com.confApi.db.clube.quemParticipa.QuemParticipa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.util.Collections;

@Service
public class QuemParticipaApi extends AbstractTransactionServiceApi implements Serializable {

    @Autowired
    private ConfAppService confAppService;

    private final RestTemplate restTemplate;

    public QuemParticipaApi(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public QuemParticipa consulta() {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/api/quemParticipa/consulta")
                    .toUriString();

            System.out.println("url: " + url);

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);

            ResponseEntity<QuemParticipa> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    QuemParticipa.class
            );

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public QuemParticipa update(Integer id, QuemParticipa quemParticipa) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/api/quemParticipa/" + id)
                    .toUriString();

            System.out.println("url: " + url);

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<QuemParticipa> entity = new HttpEntity<>(quemParticipa, headers);

            ResponseEntity<QuemParticipa> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    QuemParticipa.class
            );

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return new QuemParticipa();
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
