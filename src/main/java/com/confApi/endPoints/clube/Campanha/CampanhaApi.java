package com.confApi.endPoints.clube.Campanha;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import com.confApi.db.clube.campanha.Campanha;
import com.confApi.db.clube.campanha.dto.CampanhaRankingDTO;
import com.confApi.endPoints.clube.message.ResponseMessage;
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
public class CampanhaApi extends AbstractTransactionServiceApi implements Serializable {

    @Autowired
    private ConfAppService confAppService;

    private final RestTemplate restTemplate;

    public CampanhaApi(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseMessage create(Campanha campanha) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/api/campanha").toUriString();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Campanha> entity = new HttpEntity<>(campanha, headers);
            ResponseEntity<ResponseMessage> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, ResponseMessage.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseMessage();
        }
    }

    public List<Campanha> getAll() {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/api/campanha").toUriString();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);
            ResponseEntity<List<Campanha>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<Campanha>>() {});
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Campanha> getAllAtivas() {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/api/campanha/ativas").toUriString();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);
            ResponseEntity<List<Campanha>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<Campanha>>() {});
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Campanha> getAllAtivasStatus1() {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/api/campanha/ativas-status-1").toUriString();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);
            ResponseEntity<List<Campanha>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<Campanha>>() {});
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Campanha> getCampanhasAtivasHoje() {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/api/campanha/campanhas-ativas-hoje").toUriString();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);
            ResponseEntity<List<Campanha>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<Campanha>>() {});
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public Campanha getById(Integer id) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/api/campanha/" + id).toUriString();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);
            ResponseEntity<Campanha> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Campanha.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return new Campanha();
        }
    }

    public Campanha update(int id, Campanha campanha) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/api/campanha/" + id).toUriString();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Campanha> entity = new HttpEntity<>(campanha, headers);
            ResponseEntity<Campanha> response = restTemplate.exchange(
                    url, HttpMethod.PUT, entity, Campanha.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return new Campanha();
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
