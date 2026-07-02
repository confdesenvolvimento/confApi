package com.confApi.endPoints.clube.contabiliCampanha;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import com.confApi.db.clube.contabiliCampanha.ContabiliCampanha;
import com.confApi.endPoints.clube.message.ResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ContabiliCampanhaApi extends AbstractTransactionServiceApi implements Serializable {

    @Autowired
    private ConfAppService confAppService;

    private final RestTemplate restTemplate;

    public ContabiliCampanhaApi(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<ContabiliCampanha> getAll() {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/contabilCampanha").toUriString();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);
            ResponseEntity<List<ContabiliCampanha>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<ContabiliCampanha>>() {});
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<ContabiliCampanha> findAll(ContabiliCampanha contabiliCampanha) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/contabilCampanha/params").toUriString();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<ContabiliCampanha> entity = new HttpEntity<>(contabiliCampanha, headers);
            ResponseEntity<List<ContabiliCampanha>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<ContabiliCampanha>>() {});
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public ContabiliCampanha create(ContabiliCampanha contabiliCampanha) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/contabilCampanha").toUriString();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<ContabiliCampanha> entity = new HttpEntity<>(contabiliCampanha, headers);
            ResponseEntity<ContabiliCampanha> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, ContabiliCampanha.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return new ContabiliCampanha();
        }
    }

    public ContabiliCampanha update(Integer id, ContabiliCampanha contabiliCampanha) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/contabilCampanha/" + id).toUriString();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<ContabiliCampanha> entity = new HttpEntity<>(contabiliCampanha, headers);
            ResponseEntity<ContabiliCampanha> response = restTemplate.exchange(
                    url, HttpMethod.PUT, entity, ContabiliCampanha.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return new ContabiliCampanha();
        }
    }

    public List<ContabiliCampanha> getRanking(Integer codgCampanha) {
        System.out.println("codgCampanha ranking: " + codgCampanha); // 👈 veja o que está saindo
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/contabilCampanha/ranking")
                    .queryParam("codgCampanha", codgCampanha).toUriString();
            System.out.println("url ranking: " + url); // 👈 veja o que está saindo
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);
            ResponseEntity<List<ContabiliCampanha>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<ContabiliCampanha>>() {});
            System.out.println("res : "+response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            System.out.println("Sem ranking para campanha: " + codgCampanha);
            return new ArrayList<>(); // 👈 trata 404 como lista vazia
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public ResponseMessage delete(Integer id) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/contabilCampanha/" + id).toUriString();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);
            ResponseEntity<ResponseMessage> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, entity, ResponseMessage.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseMessage();
        }
    }

    public ResponseMessage deleteIdCampanhaAll(Integer id) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/contabilCampanha/deleteIdCampanhaAll/" + id).toUriString();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);
            ResponseEntity<ResponseMessage> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, entity, ResponseMessage.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseMessage();
        }
    }

    public List<?> relatorio(Integer campanhaId) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/contabilCampanha/relatorio/" + campanhaId).toUriString();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);
            ResponseEntity<List<?>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<?>>() {});
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
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
