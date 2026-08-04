package com.confApi.clientapp.security;

import com.confApi.clientapp.api.ClientAppSessionController;
import com.confApi.clientapp.config.ClientAppSecurityComponentsConfig;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClientAppSessionController.class)
@Import({SecurityConfig.class, ClientAppSecurityComponentsConfig.class})
@TestPropertySource(properties = "mviagens.backend.enabled=false")
class ClientAppSecurityDisabledWebMvcTest {

    @Resource
    private MockMvc mockMvc;

    @MockBean
    private UsuarioServiceImpl usuarioService;

    @MockBean
    private JwtService jwtService;

    @Test
    void disabledNamespaceFailsClosedWithoutFallingBackToLegacyJwt() throws Exception {
        mockMvc.perform(get("/api/client-app/v1/auth/session")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer internal.token.signature"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));

        verify(jwtService, never()).tokenValido(anyString());
        verify(usuarioService, never()).loadUserByUsername(anyString());
    }
}
