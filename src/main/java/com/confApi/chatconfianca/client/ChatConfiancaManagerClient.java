package com.confApi.chatconfianca.client;

import com.confApi.config.UrlConfig;
import com.confApi.exception.RegraDeNegocioException;
import com.confApi.exception.ServiceIndisponivelException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.function.Supplier;

@Service
public class ChatConfiancaManagerClient {
    private final RestTemplate restTemplate;
    private final ChatConfiancaTokenProvider tokenProvider;
    private final MeterRegistry meterRegistry;

    public ChatConfiancaManagerClient(
            @Qualifier("chatConfiancaRestTemplate") RestTemplate restTemplate,
            ChatConfiancaTokenProvider tokenProvider,
            MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.tokenProvider = tokenProvider;
        this.meterRegistry = meterRegistry;
    }

    public <T> T get(String path, Class<T> responseType) {
        try {
            ResponseEntity<T> response = exchange(path, HttpMethod.GET, null, responseType);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            return null;
        } catch (HttpStatusCodeException ex) {
            throw mapStatusException("consultar o manager", ex);
        } catch (RestClientException ex) {
            throw new ServiceIndisponivelException("Nao foi possivel consultar o manager.");
        }
    }

    public <T> List<T> getList(String path, ParameterizedTypeReference<List<T>> responseType) {
        try {
            ResponseEntity<List<T>> response = exchange(path, HttpMethod.GET, null, responseType);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (HttpClientErrorException.NotFound ex) {
            return Collections.emptyList();
        } catch (HttpStatusCodeException ex) {
            throw mapStatusException("consultar o manager", ex);
        } catch (RestClientException ex) {
            throw new ServiceIndisponivelException("Nao foi possivel consultar o manager.");
        }
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        try {
            ResponseEntity<T> response = exchange(path, HttpMethod.POST, body, responseType);
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            throw mapStatusException("gravar no manager", ex);
        } catch (RestClientException ex) {
            throw new ServiceIndisponivelException("Nao foi possivel gravar no manager.");
        }
    }

    public <T> List<T> postList(
            String path,
            Object body,
            ParameterizedTypeReference<List<T>> responseType) {
        try {
            ResponseEntity<List<T>> response = exchange(path, HttpMethod.POST, body, responseType);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (HttpStatusCodeException ex) {
            throw mapStatusException("gravar no manager", ex);
        } catch (RestClientException ex) {
            throw new ServiceIndisponivelException("Nao foi possivel gravar no manager.");
        }
    }


    public void delete(String path) {
        try {
            exchange(path, HttpMethod.DELETE, null, Void.class);
        } catch (HttpClientErrorException.NotFound ex) {
            return;
        } catch (HttpStatusCodeException ex) {
            throw mapStatusException("excluir no manager", ex);
        } catch (RestClientException ex) {
            throw new ServiceIndisponivelException("Nao foi possivel excluir no manager.");
        }
    }

    private <T> ResponseEntity<T> exchange(
            String path,
            HttpMethod method,
            Object body,
            Class<T> responseType) {
        return executeWithTokenRetry(
                () -> exchangeAttempt(path, method, body, responseType),
                path,
                method,
                body,
                responseType
        );
    }

    private <T> ResponseEntity<T> exchange(
            String path,
            HttpMethod method,
            Object body,
            ParameterizedTypeReference<T> responseType) {
        return executeWithTokenRetry(
                () -> exchangeAttempt(path, method, body, responseType),
                path,
                method,
                body,
                responseType
        );
    }

    private <T> ResponseEntity<T> executeWithTokenRetry(
            Supplier<AuthenticatedResponse<T>> firstAttempt,
            String path,
            HttpMethod method,
            Object body,
            Class<T> responseType) {
        try {
            return firstAttempt.get().response();
        } catch (ManagerUnauthorizedException ex) {
            tokenProvider.invalidateIfCurrent(ex.rejectedToken());
            try {
                return exchangeAttempt(path, method, body, responseType).response();
            } catch (ManagerUnauthorizedException secondUnauthorized) {
                tokenProvider.invalidateIfCurrent(secondUnauthorized.rejectedToken());
                throw new ServiceIndisponivelException(
                        "Nao foi possivel autenticar no manager.");
            }
        }
    }

    private <T> ResponseEntity<T> executeWithTokenRetry(
            Supplier<AuthenticatedResponse<T>> firstAttempt,
            String path,
            HttpMethod method,
            Object body,
            ParameterizedTypeReference<T> responseType) {
        try {
            return firstAttempt.get().response();
        } catch (ManagerUnauthorizedException ex) {
            tokenProvider.invalidateIfCurrent(ex.rejectedToken());
            try {
                return exchangeAttempt(path, method, body, responseType).response();
            } catch (ManagerUnauthorizedException secondUnauthorized) {
                tokenProvider.invalidateIfCurrent(secondUnauthorized.rejectedToken());
                throw new ServiceIndisponivelException(
                        "Nao foi possivel autenticar no manager.");
            }
        }
    }

    private <T> AuthenticatedResponse<T> exchangeAttempt(
            String path,
            HttpMethod method,
            Object body,
            Class<T> responseType) {
        String token = tokenProvider.bearerToken();
        try {
            ResponseEntity<T> response = measure(method, () -> restTemplate.exchange(
                    url(path),
                    method,
                    entity(body, token),
                    responseType
            ));
            return new AuthenticatedResponse<>(response, token);
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new ManagerUnauthorizedException(token);
        }
    }

    private <T> AuthenticatedResponse<T> exchangeAttempt(
            String path,
            HttpMethod method,
            Object body,
            ParameterizedTypeReference<T> responseType) {
        String token = tokenProvider.bearerToken();
        try {
            ResponseEntity<T> response = measure(method, () -> restTemplate.exchange(
                    url(path),
                    method,
                    entity(body, token),
                    responseType
            ));
            return new AuthenticatedResponse<>(response, token);
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new ManagerUnauthorizedException(token);
        }
    }

    private <T> ResponseEntity<T> measure(
            HttpMethod method,
            Supplier<ResponseEntity<T>> request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "io_error";
        try {
            ResponseEntity<T> response = request.get();
            outcome = statusOutcome(response.getStatusCodeValue());
            return response;
        } catch (HttpStatusCodeException ex) {
            outcome = statusOutcome(ex.getRawStatusCode());
            throw ex;
        } finally {
            sample.stop(Timer.builder("chatconfianca.manager.requests")
                    .tag("method", method.name())
                    .tag("outcome", outcome)
                    .register(meterRegistry));
        }
    }

    private HttpEntity<?> entity(Object body, String token) {
        HttpHeaders headers = headers(token);
        return body == null ? new HttpEntity<>(headers) : new HttpEntity<>(body, headers);
    }

    private HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);
        return headers;
    }

    private String statusOutcome(int status) {
        if (status >= 200 && status < 300) {
            return "2xx";
        }
        if (status == 401) {
            return "401";
        }
        if (status == 403) {
            return "403";
        }
        if (status >= 400 && status < 500) {
            return "4xx";
        }
        if (status >= 500) {
            return "5xx";
        }
        return "other";
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
        String message = "Solicitacao rejeitada pelo manager (HTTP "
                + ex.getRawStatusCode() + ").";
        if (ex.getStatusCode().is4xxClientError()) {
            return new RegraDeNegocioException(ex.getRawStatusCode(), message);
        }
        return new ServiceIndisponivelException("Erro ao " + action + ": " + message);
    }

    private record AuthenticatedResponse<T>(ResponseEntity<T> response, String token) {
    }

    private static final class ManagerUnauthorizedException extends RuntimeException {
        private final String rejectedToken;

        private ManagerUnauthorizedException(String rejectedToken) {
            super(null, null, false, false);
            this.rejectedToken = rejectedToken;
        }

        private String rejectedToken() {
            return rejectedToken;
        }
    }
}
