package com.confApi.clientapp.security;

import com.confApi.clientapp.config.MViagensBackendProperties;
import com.confApi.clientapp.integration.MViagensBackendClient;
import com.confApi.clientapp.integration.MViagensBackendUnavailableException;
import com.confApi.clientapp.integration.MViagensIntrospectionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientAppAuthenticationFilterTest {

    private static final String ACCESS_TOKEN = "header.payload.signature";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsServiceUnavailableWithoutCallingBackendWhenFeatureIsDisabled() throws Exception {
        MViagensBackendProperties properties = new MViagensBackendProperties();
        MViagensBackendClient backendClient = mock(MViagensBackendClient.class);
        ClientAppAuthenticationFilter filter = filter(properties, backendClient);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(requestWithBearer(), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
        verify(backendClient, never()).introspect(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createsDedicatedClientPrincipalForActiveSession() throws Exception {
        MViagensBackendProperties properties = enabledProperties();
        MViagensBackendClient backendClient = mock(MViagensBackendClient.class);
        when(backendClient.introspect(ACCESS_TOKEN, "request-1")).thenReturn(activeResponse());
        ClientAppAuthenticationFilter filter = filter(properties, backendClient);
        MockHttpServletRequest request = requestWithBearer();
        request.addHeader(ClientAppCorrelationIdFilter.CORRELATION_ID_HEADER, "request-1");
        request.setAttribute(ClientAppCorrelationIdFilter.CORRELATION_ID_ATTRIBUTE, "request-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isInstanceOf(ClientAppAuthenticationToken.class);
        ClientAppPrincipal principal = (ClientAppPrincipal)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.activeAgencyId()).isEqualTo(10);
        verify(backendClient).introspect(ACCESS_TOKEN, "request-1");
    }

    @Test
    void mapsInactiveSessionToUniformUnauthorizedResponse() throws Exception {
        MViagensBackendProperties properties = enabledProperties();
        MViagensBackendClient backendClient = mock(MViagensBackendClient.class);
        when(backendClient.introspect(org.mockito.ArgumentMatchers.eq(ACCESS_TOKEN), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new MViagensIntrospectionResponse(
                        false, null, null, null, null, null, null, null, List.of()));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter(properties, backendClient).doFilter(requestWithBearer(), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).contains("invalid_token");
        assertThat(response.getContentAsString()).doesNotContain(ACCESS_TOKEN);
    }

    @Test
    void mapsTechnicalFailureToServiceUnavailable() throws Exception {
        MViagensBackendProperties properties = enabledProperties();
        MViagensBackendClient backendClient = mock(MViagensBackendClient.class);
        when(backendClient.introspect(org.mockito.ArgumentMatchers.eq(ACCESS_TOKEN), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new MViagensBackendUnavailableException(
                        MViagensBackendUnavailableException.Reason.TECHNICAL_AUTHENTICATION));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter(properties, backendClient).doFilter(requestWithBearer(), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString())
                .contains("SERVICE_UNAVAILABLE")
                .doesNotContain("TECHNICAL_AUTHENTICATION");
    }

    @SuppressWarnings("unchecked")
    private ClientAppAuthenticationFilter filter(
            MViagensBackendProperties properties,
            MViagensBackendClient backendClient
    ) {
        ObjectProvider<MViagensBackendClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(backendClient);
        return new ClientAppAuthenticationFilter(
                properties,
                provider,
                new ClientAppSecurityErrorWriter(new ObjectMapper())
        );
    }

    private MViagensBackendProperties enabledProperties() {
        MViagensBackendProperties properties = new MViagensBackendProperties();
        properties.setEnabled(true);
        return properties;
    }

    private MockHttpServletRequest requestWithBearer() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/client-app/v1/auth/session");
        request.setServletPath("/api/client-app/v1/auth/session");
        request.addHeader("Authorization", "Bearer " + ACCESS_TOKEN);
        return request;
    }

    private MViagensIntrospectionResponse activeResponse() {
        return new MViagensIntrospectionResponse(
                true,
                "0f80237d-89f4-4ef0-98c5-ea05ab5b6e42",
                "be0e4628-c76a-433b-97cc-5e87c677fe7e",
                "0695d86a-4621-45ce-8f30-9d246cfbd28d",
                "1237aa91-2854-4689-a08d-0ce4fe797546",
                10,
                2L,
                3L,
                List.of("customer:authenticated")
        );
    }
}
