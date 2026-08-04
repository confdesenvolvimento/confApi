package com.confApi.clientapp.api;

import com.confApi.clientapp.security.ClientAppAuthenticationToken;
import com.confApi.clientapp.security.ClientAppPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClientAppSessionControllerTest {

    @Test
    void returnsOnlySanitizedPublicSessionFieldsWithoutCache() throws Exception {
        ClientAppPrincipal principal = new ClientAppPrincipal(
                "0f80237d-89f4-4ef0-98c5-ea05ab5b6e42",
                "be0e4628-c76a-433b-97cc-5e87c677fe7e",
                "0695d86a-4621-45ce-8f30-9d246cfbd28d",
                "1237aa91-2854-4689-a08d-0ce4fe797546",
                10,
                2L,
                3L,
                List.of("customer:authenticated")
        );
        ClientAppAuthenticationToken auth = new ClientAppAuthenticationToken(
                principal,
                List.of(new SimpleGrantedAuthority("SCOPE_customer:authenticated"))
        );
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ClientAppSessionController())
                .build();

        mockMvc.perform(get("/api/client-app/v1/auth/session").principal(auth))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
                .andExpect(header().string(HttpHeaders.VARY, HttpHeaders.AUTHORIZATION))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.sessionId").value(principal.sessionPublicId()))
                .andExpect(jsonPath("$.customerId").value(principal.customerPublicId()))
                .andExpect(jsonPath("$.activeAgencyId").value("10"))
                .andExpect(jsonPath("$.contextVersion").value("3"))
                .andExpect(jsonPath("$.devicePublicId").doesNotExist())
                .andExpect(jsonPath("$.agencyLinkPublicId").doesNotExist())
                .andExpect(jsonPath("$.sessionVersion").doesNotExist());
    }
}
