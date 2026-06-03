package com.confApi.endPoints.clube.arquivoAnexo;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import com.confApi.db.clube.arquivoAnexo.ArquivoAnexo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ArquivoAnexoApi extends AbstractTransactionServiceApi implements Serializable {

    @Autowired
    private ConfAppService confAppService;

    private final RestTemplate restTemplate;

    public ArquivoAnexoApi(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ArquivoAnexo create(ArquivoAnexo arquivoAnexo) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/api/arquivo")
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<ArquivoAnexo> entity = new HttpEntity<>(arquivoAnexo, headers);

            ResponseEntity<ArquivoAnexo> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, ArquivoAnexo.class);

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return new ArquivoAnexo();
        }
    }

    public List<ArquivoAnexo> getAll() {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/api/arquivo")
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);

            ResponseEntity<List<ArquivoAnexo>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<ArquivoAnexo>>() {});

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<ArquivoAnexo> findAll(ArquivoAnexo arquivoAnexo) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/api/arquivo/filter")
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<ArquivoAnexo> entity = new HttpEntity<>(arquivoAnexo, headers);

            ResponseEntity<List<ArquivoAnexo>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<ArquivoAnexo>>() {});

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public ArquivoAnexo update(int id, ArquivoAnexo arquivoAnexo) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/api/arquivo/" + id)
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<ArquivoAnexo> entity = new HttpEntity<>(arquivoAnexo, headers);

            ResponseEntity<ArquivoAnexo> response = restTemplate.exchange(
                    url, HttpMethod.PUT, entity, ArquivoAnexo.class);

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return new ArquivoAnexo();
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
