package com.confApi.clientapp.integration;

import com.confApi.clientapp.config.MViagensBackendProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MViagensBackendClientTest {

    private static final String SERVICE_TOKEN = "service-token-with-at-least-32-characters";
    private static final String ACCESS_TOKEN = "header.payload.signature";
    private static final String CORRELATION_ID = "request-123";
    private static final String ENDPOINT = "http://127.0.0.1:8091/internal/v1/auth/introspect";

    private MockRestServiceServer server;
    private MViagensBackendClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();

        MViagensBackendProperties properties = new MViagensBackendProperties();
        properties.setEnabled(true);
        properties.setBaseUrl(URI.create("http://127.0.0.1:8091"));
        properties.setServiceToken(SERVICE_TOKEN);
        client = new MViagensBackendClient(restTemplate, properties);
    }

    @Test
    void sendsOnlyConfiguredTechnicalIdentityAndReturnsValidatedContext() {
        server.expect(once(), requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(MViagensBackendClient.SERVICE_TOKEN_HEADER, SERVICE_TOKEN))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(header(MViagensBackendClient.CORRELATION_ID_HEADER, CORRELATION_ID))
                .andRespond(withSuccess(activeResponse(), MediaType.APPLICATION_JSON));

        MViagensIntrospectionResponse response = client.introspect(ACCESS_TOKEN, CORRELATION_ID);

        assertThat(response.active()).isTrue();
        assertThat(response.activeAgencyId()).isEqualTo(10);
        assertThat(response.contextVersion()).isEqualTo(3L);
        server.verify();
    }

    @Test
    void mapsInactiveResponseWithoutCreatingContext() {
        server.expect(once(), requestTo(ENDPOINT))
                .andRespond(withSuccess("{\"active\":false}", MediaType.APPLICATION_JSON));

        assertThat(client.introspect(ACCESS_TOKEN, CORRELATION_ID).active()).isFalse();
        server.verify();
    }

    @Test
    void rejectsIncompleteActiveResponse() {
        server.expect(once(), requestTo(ENDPOINT))
                .andRespond(withSuccess("{\"active\":true}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.introspect(ACCESS_TOKEN, CORRELATION_ID))
                .isInstanceOf(MViagensBackendUnavailableException.class)
                .extracting("reason")
                .isEqualTo(MViagensBackendUnavailableException.Reason.MALFORMED_RESPONSE);
        server.verify();
    }

    @Test
    void mapsBackendUnauthorizedToTechnicalUnavailability() {
        server.expect(once(), requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"must-not-be-propagated\"}"));

        assertThatThrownBy(() -> client.introspect(ACCESS_TOKEN, CORRELATION_ID))
                .isInstanceOf(MViagensBackendUnavailableException.class)
                .hasMessage("mViagensBackend unavailable")
                .extracting("reason")
                .isEqualTo(MViagensBackendUnavailableException.Reason.TECHNICAL_AUTHENTICATION);
        server.verify();
    }

    @Test
    void rejectsMalformedJwtWithoutCallingBackend() {
        MViagensIntrospectionResponse response = client.introspect("not a jwt", CORRELATION_ID);

        assertThat(response.active()).isFalse();
        server.verify();
    }

    private String activeResponse() {
        return "{" +
                "\"active\":true," +
                "\"customerPublicId\":\"0f80237d-89f4-4ef0-98c5-ea05ab5b6e42\"," +
                "\"sessionPublicId\":\"be0e4628-c76a-433b-97cc-5e87c677fe7e\"," +
                "\"devicePublicId\":\"0695d86a-4621-45ce-8f30-9d246cfbd28d\"," +
                "\"agencyLinkPublicId\":\"1237aa91-2854-4689-a08d-0ce4fe797546\"," +
                "\"activeAgencyId\":10," +
                "\"sessionVersion\":2," +
                "\"contextVersion\":3," +
                "\"scopes\":[\"customer:authenticated\"]" +
                "}";
    }
}
