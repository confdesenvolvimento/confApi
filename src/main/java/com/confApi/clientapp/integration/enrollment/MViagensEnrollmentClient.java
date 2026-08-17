package com.confApi.clientapp.integration.enrollment;

import com.confApi.clientapp.config.MViagensBackendProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

public class MViagensEnrollmentClient {
    private static final Logger LOG = LoggerFactory.getLogger(MViagensEnrollmentClient.class);
    private static final Pattern SAFE_PROBLEM_CODE = Pattern.compile("^[A-Z0-9_]{1,80}$");
    private final RestTemplate restTemplate;
    private final MViagensBackendProperties properties;
    private final ObjectMapper objectMapper;

    public MViagensEnrollmentClient(RestTemplate restTemplate, MViagensBackendProperties properties, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public JsonNode createFlow(Map<String, Object> body, String idempotencyKey, String requestId, String correlationId) {
        return exchange(HttpMethod.POST, "/internal/v1/auth/cpf/flows", body, idempotencyKey, requestId, correlationId);
    }

    public JsonNode submitCandidates(String flowId, Map<String, Object> body, String idempotencyKey, String requestId, String correlationId) {
        return exchange(HttpMethod.PUT, "/internal/v1/auth/cpf/flows/" + safe(flowId) + "/agency-candidates", body, idempotencyKey, requestId, correlationId);
    }

    public JsonNode selectAgency(String flowId, Map<String, Object> body, String idempotencyKey, String requestId, String correlationId) {
        return exchange(HttpMethod.PUT, "/internal/v1/auth/cpf/flows/" + safe(flowId) + "/agency", body, idempotencyKey, requestId, correlationId);
    }

    public JsonNode verify(String challengeId, Map<String, Object> body, String idempotencyKey, String requestId, String correlationId) {
        return exchange(HttpMethod.POST, "/internal/v1/auth/cpf/challenges/" + safe(challengeId) + "/verify", body, idempotencyKey, requestId, correlationId);
    }

    public JsonNode resend(String challengeId, String idempotencyKey, String requestId, String correlationId) {
        return exchange(HttpMethod.POST, "/internal/v1/auth/cpf/challenges/" + safe(challengeId) + "/resend", null, idempotencyKey, requestId, correlationId);
    }

    public JsonNode refresh(Map<String, Object> body, String idempotencyKey, String requestId, String correlationId) {
        return exchange(HttpMethod.POST, "/internal/v1/auth/sessions/refresh", body, idempotencyKey, requestId, correlationId);
    }

    public JsonNode listPublicAgencies(String requestId, String correlationId) {
        return exchange(HttpMethod.GET, "/internal/v1/agencies/directory", null,
                UUID.randomUUID().toString(), requestId, correlationId);
    }

    private JsonNode exchange(HttpMethod method, String path, Object body, String idempotencyKey,
                              String requestId, String correlationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("X-MViagens-Service-Token", properties.getServiceToken());
        headers.set("X-Correlation-Id", safeHeader(correlationId));
        if (requestId != null && !requestId.isBlank()) headers.set("X-Request-Id", safeHeader(requestId));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) headers.set("Idempotency-Key", safeHeader(idempotencyKey));
        headers.setCacheControl("no-store"); headers.setPragma("no-cache");
        try {
            long started = System.nanoTime();
            ResponseEntity<String> response = restTemplate.exchange(uri(path), method,
                    new HttpEntity<>(body == null ? null : objectMapper.writeValueAsString(body), headers), String.class);
            if (response.getBody() == null || response.getBody().isBlank()) throw unavailable();
            LOG.info("mViagens call method={} path={} status={} correlationId={} elapsedMs={}", method, path, response.getStatusCode().value(), correlationId, (System.nanoTime() - started) / 1_000_000L);
            return objectMapper.readTree(response.getBody());
        } catch (HttpStatusCodeException exception) {
            int status = exception.getStatusCode().value();
            String code = problemCode(exception.getResponseBodyAsString());
            boolean retryable = problemRetryable(exception.getResponseBodyAsString(), status >= 500 || status == 429);
            LOG.warn("mViagens call rejected method={} path={} status={} code={} correlationId={}",
                    method, path, status, code, correlationId);
            if (Set.of(400, 404, 409, 410, 422, 429, 503).contains(status)) {
                throw new ClientAppEnrollmentException(status, code, retryable);
            }
            throw unavailable();
        } catch (ResourceAccessException | java.io.IOException exception) {
            LOG.error("mViagens call unavailable method={} path={} correlationId={} error={}", method, path, correlationId, exception.getClass().getSimpleName());
            throw unavailable();
        } catch (RestClientException exception) {
            LOG.error("mViagens call failed method={} path={} correlationId={} error={}", method, path, correlationId, exception.getClass().getSimpleName());
            throw unavailable();
        }
    }

    private URI uri(String path) { return UriComponentsBuilder.fromUri(properties.getBaseUrl()).path(path).build().toUri(); }
    private String safe(String value) { if (value == null || !value.matches("[0-9a-fA-F-]{1,100}")) throw new ClientAppEnrollmentException(400, "INVALID_IDENTIFIER", false); return value; }
    private String safeHeader(String value) { return value == null || value.isBlank() ? UUID.randomUUID().toString() : value.replaceAll("[^A-Za-z0-9._-]", "").substring(0, Math.min(100, value.replaceAll("[^A-Za-z0-9._-]", "").length())); }
    private String problemCode(String body) {
        try {
            JsonNode value = objectMapper.readTree(body);
            String code = value.path("code").asText("");
            return SAFE_PROBLEM_CODE.matcher(code).matches() ? code : "ENROLLMENT_REQUEST_REJECTED";
        } catch (java.io.IOException exception) {
            return "ENROLLMENT_REQUEST_REJECTED";
        }
    }
    private boolean problemRetryable(String body, boolean fallback) {
        try {
            JsonNode value = objectMapper.readTree(body);
            return value.has("retryable") ? value.path("retryable").asBoolean(fallback) : fallback;
        } catch (java.io.IOException exception) {
            return fallback;
        }
    }
    private ClientAppEnrollmentException unavailable() { return new ClientAppEnrollmentException(503, "ENROLLMENT_SERVICE_UNAVAILABLE", true); }
}
