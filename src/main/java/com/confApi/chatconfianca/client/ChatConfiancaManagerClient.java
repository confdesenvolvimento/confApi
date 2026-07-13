package com.confApi.chatconfianca.client;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.exception.RegraDeNegocioException;
import com.confApi.exception.ServiceIndisponivelException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
public class ChatConfiancaManagerClient {
    private final RestTemplate restTemplate;
    private final ConfAppService confAppService;

    public ChatConfiancaManagerClient(RestTemplate restTemplate, ConfAppService confAppService) {
        this.restTemplate = restTemplate;
        this.confAppService = confAppService;
    }

    public <T> T get(String path, Class<T> responseType) {
        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    url(path),
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    responseType
            );
            return response.getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            return null;
        } catch (HttpStatusCodeException ex) {
            throw mapStatusException("consultar o manager", ex);
        } catch (RestClientException ex) {
            throw new ServiceIndisponivelException("Nao foi possivel consultar o manager: " + ex.getMessage());
        }
    }

    public <T> List<T> getList(String path, ParameterizedTypeReference<List<T>> responseType) {
        try {
            ResponseEntity<List<T>> response = restTemplate.exchange(
                    url(path),
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    responseType
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (HttpClientErrorException.NotFound ex) {
            return Collections.emptyList();
        } catch (HttpStatusCodeException ex) {
            throw mapStatusException("consultar o manager", ex);
        } catch (RestClientException ex) {
            throw new ServiceIndisponivelException("Nao foi possivel consultar o manager: " + ex.getMessage());
        }
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    url(path),
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers()),
                    responseType
            );
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            throw mapStatusException("gravar no manager", ex);
        } catch (RestClientException ex) {
            throw new ServiceIndisponivelException("Nao foi possivel gravar no manager: " + ex.getMessage());
        }
    }


    public void delete(String path) {
        try {
            restTemplate.exchange(
                    url(path),
                    HttpMethod.DELETE,
                    new HttpEntity<>(headers()),
                    Void.class
            );
        } catch (HttpClientErrorException.NotFound ex) {
            return;
        } catch (HttpStatusCodeException ex) {
            throw mapStatusException("excluir no manager", ex);
        } catch (RestClientException ex) {
            throw new ServiceIndisponivelException("Nao foi possivel excluir no manager: " + ex.getMessage());
        }
    }

    private HttpHeaders headers() {
        ConfAppResp token = confAppService.token();
        if (token == null || token.getToken() == null || token.getToken().isBlank()) {
            throw new ServiceIndisponivelException("Nao foi possivel obter token para chamar o manager.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token.getToken());
        return headers;
    }

    private String url(String path) {
        if (UrlConfig.URL_CONFIANCA_MANAGER == null || UrlConfig.URL_CONFIANCA_MANAGER.isBlank()) {
            throw new ServiceIndisponivelException("URL do confianca-manager nao configurada.");
        }

        String base = UrlConfig.URL_CONFIANCA_MANAGER.endsWith("/")
                ? UrlConfig.URL_CONFIANCA_MANAGER
                : UrlConfig.URL_CONFIANCA_MANAGER + "/";
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        return base + normalizedPath;
    }

    private RuntimeException mapStatusException(String action, HttpStatusCodeException ex) {
        String message = extractMessage(ex);
        if (ex.getStatusCode().is4xxClientError()) {
            return new RegraDeNegocioException(ex.getRawStatusCode(), message);
        }
        return new ServiceIndisponivelException("Erro ao " + action + ": " + message);
    }

    private String extractMessage(HttpStatusCodeException ex) {
        String body = ex.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            return body.length() > 500 ? body.substring(0, 500) : body;
        }
        return ex.getMessage();
    }
}