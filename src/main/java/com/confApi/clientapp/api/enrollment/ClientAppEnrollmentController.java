package com.confApi.clientapp.api.enrollment;

import com.confApi.clientapp.security.ClientAppCorrelationIdFilter;
import com.confApi.clientapp.security.ClientAppSecurityErrorWriter;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/client-app/v1/public/auth")
public class ClientAppEnrollmentController {
    private static final Pattern IDEMPOTENCY = Pattern.compile("^[A-Za-z0-9._:-]{16,128}$");
    private final ClientAppEnrollmentService service;
    public ClientAppEnrollmentController(ClientAppEnrollmentService service) { this.service = service; }

    @PostMapping("/cpf/flows")
    public ResponseEntity<JsonNode> start(@RequestHeader("Idempotency-Key") String idem, @RequestHeader(value="X-Request-Id", required=false) String requestId, @RequestBody ClientAppEnrollmentService.StartRequest request, HttpServletRequest servletRequest) {
        return ok(service.start(request, validateIdempotency(idem), requestId, ClientAppCorrelationIdFilter.from(servletRequest)), servletRequest);
    }
    @PutMapping("/cpf/flows/{flowId}/agency")
    public ResponseEntity<JsonNode> select(@PathVariable String flowId, @RequestHeader("Idempotency-Key") String idem, @RequestHeader(value="X-Request-Id", required=false) String requestId, @RequestBody ClientAppEnrollmentService.SelectAgencyRequest request, HttpServletRequest servletRequest) {
        return ok(service.select(flowId, request, validateIdempotency(idem), requestId, ClientAppCorrelationIdFilter.from(servletRequest)), servletRequest);
    }
    @PostMapping("/cpf/challenges/{challengeId}/verify")
    public ResponseEntity<JsonNode> verify(@PathVariable String challengeId, @RequestHeader("Idempotency-Key") String idem, @RequestHeader(value="X-Request-Id", required=false) String requestId, @RequestBody ClientAppEnrollmentService.VerifyOtpRequest request, HttpServletRequest servletRequest) {
        return ok(service.verify(challengeId, request, validateIdempotency(idem), requestId, ClientAppCorrelationIdFilter.from(servletRequest)), servletRequest);
    }
    @PostMapping("/cpf/challenges/{challengeId}/resend")
    public ResponseEntity<JsonNode> resend(@PathVariable String challengeId, @RequestHeader("Idempotency-Key") String idem, @RequestHeader(value="X-Request-Id", required=false) String requestId, HttpServletRequest servletRequest) {
        return ok(service.resend(challengeId, validateIdempotency(idem), requestId, ClientAppCorrelationIdFilter.from(servletRequest)), servletRequest);
    }
    @PostMapping("/sessions/refresh")
    public ResponseEntity<JsonNode> refresh(@RequestHeader("Idempotency-Key") String idem, @RequestHeader(value="X-Request-Id", required=false) String requestId, @RequestBody ClientAppEnrollmentService.RefreshSessionRequest request, HttpServletRequest servletRequest) {
        return ok(service.refresh(request, validateIdempotency(idem), requestId, ClientAppCorrelationIdFilter.from(servletRequest)), servletRequest);
    }
    private ResponseEntity<JsonNode> ok(JsonNode body, HttpServletRequest request) {
        String correlation = ClientAppCorrelationIdFilter.from(request);
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON); ClientAppSecurityErrorWriter.applyNoStoreHeaders(headers, correlation);
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }
    private String validateIdempotency(String value) {
        if (value == null || !IDEMPOTENCY.matcher(value).matches()) {
            throw new com.confApi.clientapp.integration.enrollment.ClientAppEnrollmentException(400, "INVALID_IDEMPOTENCY_KEY", false);
        }
        return value;
    }
}
