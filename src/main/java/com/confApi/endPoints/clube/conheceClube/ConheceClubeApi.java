package com.confApi.endPoints.clube.conheceClube;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import com.confApi.db.clube.conhecaClube.ConhecaClube;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.util.Collections;

@Service
public class ConheceClubeApi extends AbstractTransactionServiceApi implements Serializable {

    @Autowired
    private ConfAppService confAppService;

    private final RestTemplate restTemplate;

    public ConheceClubeApi(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    public ConhecaClube consulta(Integer id) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/api/conhecaClube/consulta/" + id)
                    .toUriString();

            System.out.println("url: " + url);

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);

            ResponseEntity<ConhecaClube> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ConhecaClube.class
            );

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return new ConhecaClube();
        }
    }

    public ConhecaClube update(int id, ConhecaClube conhecaClube) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/api/conhecaClube/" + id)
                    .toUriString();

            System.out.println("url: " + url);

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<ConhecaClube> entity = new HttpEntity<>(conhecaClube, headers);

            ResponseEntity<ConhecaClube> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    ConhecaClube.class
            );

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return new ConhecaClube();
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
