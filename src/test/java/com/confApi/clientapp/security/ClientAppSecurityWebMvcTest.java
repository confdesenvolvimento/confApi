package com.confApi.clientapp.security;

import com.confApi.clientapp.api.ClientAppSessionController;
import com.confApi.clientapp.config.ClientAppSecurityComponentsConfig;
import com.confApi.clientapp.integration.MViagensBackendClient;
import com.confApi.clientapp.integration.MViagensBackendUnavailableException;
import com.confApi.clientapp.integration.MViagensIntrospectionResponse;
import com.confApi.config.SecurityConfig;
import com.confApi.security.jwt.JwtService;
import com.confApi.service.UsuarioServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.annotation.Resource;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClientAppSessionController.class)
@Import({SecurityConfig.class, ClientAppSecurityComponentsConfig.class})
@TestPropertySource(properties = {
        "mviagens.backend.enabled=true",
        "mviagens.backend.base-url=http://127.0.0.1:8091",
        "mviagens.backend.service-token=service-token-with-at-least-32-characters"
})
class ClientAppSecurityWebMvcTest {

    @Resource
    private MockMvc mockMvc;

    @MockBean
    private MViagensBackendClient backendClient;

    @MockBean
    private UsuarioServiceImpl usuarioService;

    @MockBean
    private JwtService jwtService;

    @Test
    void returnsUnauthorizedWithoutBearerAndDoesNotUseLegacyJwt() throws Exception {
        mockMvc.perform(get("/api/client-app/v1/auth/session"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE,
                        "Bearer realm=\"client-app\", error=\"invalid_token\""))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        verify(backendClient, never()).introspect(anyString(), anyString());
        verify(jwtService, never()).tokenValido(anyString());
    }

    @Test
    void authenticatesActiveClientExactlyOnceAndReturnsSession() throws Exception {
        when(backendClient.introspect(eq("header.payload.signature"), eq("request-42")))
                .thenReturn(activeResponse());

        mockMvc.perform(get("/api/client-app/v1/auth/session")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer header.payload.signature")
                        .header(ClientAppCorrelationIdFilter.CORRELATION_ID_HEADER, "request-42"))
                .andExpect(status().isOk())
                .andExpect(header().string(ClientAppCorrelationIdFilter.CORRELATION_ID_HEADER, "request-42"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL,
                        "no-store, no-cache, max-age=0, must-revalidate"))
                .andExpect(jsonPath("$.customerId").value("0f80237d-89f4-4ef0-98c5-ea05ab5b6e42"))
                .andExpect(jsonPath("$.activeAgencyId").value("10"))
                .andExpect(jsonPath("$.contextVersion").value("3"));

        verify(backendClient).introspect("header.payload.signature", "request-42");
        verify(jwtService, never()).tokenValido(anyString());
    }

    @Test
    void mapsInactiveSessionToUnauthorized() throws Exception {
        when(backendClient.introspect(eq("header.payload.signature"), anyString()))
                .thenReturn(new MViagensIntrospectionResponse(
                        false, null, null, null, null, null, null, null, List.of()));

        mockMvc.perform(get("/api/client-app/v1/auth/session")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer header.payload.signature"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void failsClosedWhenBackendIsUnavailable() throws Exception {
        when(backendClient.introspect(eq("header.payload.signature"), anyString()))
                .thenThrow(new MViagensBackendUnavailableException(
                        MViagensBackendUnavailableException.Reason.CONNECTIVITY_OR_TIMEOUT));

        mockMvc.perform(get("/api/client-app/v1/auth/session")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer header.payload.signature"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));
    }

    @Test
    void rejectsDuplicatedAuthorizationHeadersBeforeIntrospection() throws Exception {
        mockMvc.perform(get("/api/client-app/v1/auth/session")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer header.payload.signature",
                                "Bearer other.payload.signature"))
                .andExpect(status().isUnauthorized());

        verify(backendClient, never()).introspect(anyString(), anyString());
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
