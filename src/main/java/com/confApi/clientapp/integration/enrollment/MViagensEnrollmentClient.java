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

import java.net.URI;
import java.util.*;

public class MViagensEnrollmentClient {
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
            ResponseEntity<String> response = restTemplate.exchange(uri(path), method,
                    new HttpEntity<>(body == null ? null : objectMapper.writeValueAsString(body), headers), String.class);
            if (response.getBody() == null || response.getBody().isBlank()) throw unavailable();
            return objectMapper.readTree(response.getBody());
        } catch (HttpStatusCodeException exception) {
            int status = exception.getStatusCode().value();
            if (status == 400 || status == 404 || status == 409 || status == 422) {
                throw new ClientAppEnrollmentException(status, "ENROLLMENT_REQUEST_REJECTED", false);
            }
            throw unavailable();
        } catch (ResourceAccessException | java.io.IOException exception) {
            throw unavailable();
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private URI uri(String path) { return UriComponentsBuilder.fromUri(properties.getBaseUrl()).path(path).build().toUri(); }
    private String safe(String value) { if (value == null || !value.matches("[0-9a-fA-F-]{1,100}")) throw new ClientAppEnrollmentException(400, "INVALID_IDENTIFIER", false); return value; }
    private String safeHeader(String value) { return value == null || value.isBlank() ? UUID.randomUUID().toString() : value.replaceAll("[^A-Za-z0-9._-]", "").substring(0, Math.min(100, value.replaceAll("[^A-Za-z0-9._-]", "").length())); }
    private ClientAppEnrollmentException unavailable() { return new ClientAppEnrollmentException(503, "ENROLLMENT_SERVICE_UNAVAILABLE", true); }
}
