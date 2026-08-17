package com.confApi.clientapp.api.enrollment;

import com.confApi.clientapp.integration.enrollment.ClientAppEnrollmentException;
import com.confApi.clientapp.security.ClientAppCorrelationIdFilter;
import com.confApi.clientapp.security.ClientAppSecurityErrorWriter;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice(assignableTypes = {
        ClientAppEnrollmentController.class,
        ClientAppAgencyDirectoryController.class
})
@Order(0)
public class ClientAppEnrollmentErrorHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ClientAppEnrollmentErrorHandler.class);
    @ExceptionHandler(ClientAppEnrollmentException.class)
    public ResponseEntity<Map<String,Object>> handle(ClientAppEnrollmentException exception, HttpServletRequest request) {
        LOG.warn("B2C request rejected status={} code={} correlationId={}", exception.getStatus(), exception.getCode(), ClientAppCorrelationIdFilter.from(request));
        return response(exception.getStatus(), exception.getCode(), exception.isRetryable(), request);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        return response(400, "VALIDATION_ERROR", false, request);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> generic(Exception exception, HttpServletRequest request) {
        LOG.error("B2C unexpected error correlationId={} error={}", ClientAppCorrelationIdFilter.from(request), exception.getClass().getSimpleName());
        return response(503, "ENROLLMENT_SERVICE_UNAVAILABLE", true, request);
    }
    private ResponseEntity<Map<String,Object>> response(int status, String code, boolean retryable, HttpServletRequest request) {
        String correlation = ClientAppCorrelationIdFilter.from(request);
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON); ClientAppSecurityErrorWriter.applyNoStoreHeaders(headers, correlation);
        Map<String,Object> body = new LinkedHashMap<>(); body.put("type", "urn:confapi:client-enrollment"); body.put("title", status >= 500 ? "Enrollment unavailable" : "Enrollment request rejected"); body.put("status", status); body.put("code", code); body.put("retryable", retryable); body.put("correlationId", correlation);
        return new ResponseEntity<>(body, headers, HttpStatus.valueOf(status));
    }
}
