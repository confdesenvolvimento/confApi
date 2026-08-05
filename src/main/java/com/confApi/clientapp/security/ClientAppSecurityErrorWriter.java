package com.confApi.clientapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientAppSecurityErrorWriter {

    private static final String CACHE_CONTROL = "no-store, no-cache, max-age=0, must-revalidate";

    private final ObjectMapper objectMapper;

    public ClientAppSecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void unauthorized(HttpServletResponse response, String correlationId) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer realm=\"client-app\", error=\"invalid_token\"");
        write(response, 401, "Unauthorized", "AUTHENTICATION_REQUIRED", correlationId);
    }

    public void forbidden(HttpServletResponse response, String correlationId) throws IOException {
        write(response, 403, "Forbidden", "OPERATION_FORBIDDEN", correlationId);
    }

    public void unavailable(HttpServletResponse response, String correlationId) throws IOException {
        write(response, 503, "Service Unavailable", "SERVICE_UNAVAILABLE", correlationId);
    }

    private void write(
            HttpServletResponse response,
            int status,
            String title,
            String code,
            String correlationId
    ) throws IOException {
        applyNoStoreHeaders(response, correlationId);
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", title);
        body.put("status", status);
        body.put("code", code);
        body.put("correlationId", correlationId);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    public static void applyNoStoreHeaders(HttpServletResponse response, String correlationId) {
        response.setHeader("Cache-Control", CACHE_CONTROL);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setHeader("Vary", "Authorization");
        response.setHeader(ClientAppCorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
    }

    public static void applyNoStoreHeaders(HttpHeaders headers, String correlationId) {
        headers.set("Cache-Control", CACHE_CONTROL);
        headers.set("Pragma", "no-cache");
        headers.set("Expires", "0");
        headers.set("Vary", "Authorization");
        headers.set(ClientAppCorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
    }
}
