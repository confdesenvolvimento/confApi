package com.confApi.clientapp.integration.enrollment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.confApi.clientapp.config.MViagensBackendProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class MViagensEnrollmentClientTest {

    @Test
    void preservesSafeOtpDeliveryProblemFromBackend() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        MViagensBackendProperties properties = new MViagensBackendProperties();
        properties.setEnabled(true);
        properties.setBaseUrl(URI.create("http://127.0.0.1:8091"));
        properties.setServiceToken("technical-token-with-at-least-32-characters");
        MViagensEnrollmentClient client =
                new MViagensEnrollmentClient(restTemplate, properties, new ObjectMapper());

        server.expect(requestTo("http://127.0.0.1:8091/internal/v1/auth/cpf/flows"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"code\":\"OTP_SMS_UNAVAILABLE\",\"retryable\":true}"));

        assertThatThrownBy(() -> client.createFlow(
                Map.of("cpf", "52998224725"),
                "idempotency-key-123456",
                "request-test",
                "correlation-test"
        )).isInstanceOfSatisfying(ClientAppEnrollmentException.class, exception -> {
            org.assertj.core.api.Assertions.assertThat(exception.getStatus()).isEqualTo(503);
            org.assertj.core.api.Assertions.assertThat(exception.getCode()).isEqualTo("OTP_SMS_UNAVAILABLE");
            org.assertj.core.api.Assertions.assertThat(exception.isRetryable()).isTrue();
        });
        server.verify();
    }
}
