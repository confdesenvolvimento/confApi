package com.confApi.endPoints.agencia;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;

import com.confApi.db.confManager.agencia.dto.Agencia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.util.Collections;

@Service
public class AgenciaApi extends AbstractTransactionServiceApi implements Serializable {


    @Autowired
    private ConfAppService confAppService;

    private final RestTemplate restTemplate;

    public AgenciaApi(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
        }

    public com.confApi.db.confManager.agencia.dto.Agencia findByIdWoobaAgencia(Integer idWoobaAgencia) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                    .path("/agencia/findByIdWoobaAgencia/" + idWoobaAgencia)
                    .toUriString();

            System.out.println("url: " + url);

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);

            ResponseEntity<com.confApi.db.confManager.agencia.dto.Agencia> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    com.confApi.db.confManager.agencia.dto.Agencia.class
            );

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return new Agencia();
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