package com.confApi.clientapp.api.enrollment;

import com.confApi.clientapp.security.ClientAppCorrelationIdFilter;
import com.confApi.clientapp.security.ClientAppSecurityErrorWriter;
import com.confApi.clientapp.integration.enrollment.MViagensEnrollmentClient;
import com.confApi.clientapp.integration.enrollment.ClientAppEnrollmentException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/client-app/v1/public/agencies")
public class ClientAppAgencyDirectoryController {

    private final ObjectProvider<MViagensEnrollmentClient> client;

    public ClientAppAgencyDirectoryController(ObjectProvider<MViagensEnrollmentClient> client) {
        this.client = client;
    }

    @GetMapping
    public ResponseEntity<JsonNode> list(HttpServletRequest request) {
        String correlationId = ClientAppCorrelationIdFilter.from(request);
        MViagensEnrollmentClient backend = client.getIfAvailable();
        if (backend == null) {
            throw new ClientAppEnrollmentException(503, "ENROLLMENT_SERVICE_UNAVAILABLE", true);
        }
        JsonNode body = backend.listPublicAgencies(
                request.getHeader("X-Request-Id"), correlationId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ClientAppSecurityErrorWriter.applyNoStoreHeaders(headers, correlationId);
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }
}
