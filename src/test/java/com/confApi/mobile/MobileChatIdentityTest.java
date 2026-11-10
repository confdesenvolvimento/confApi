package com.confApi.mobile;

import com.confApi.chatconfianca.dto.model.RefUsuario;
import com.confApi.chatconfianca.service.ChatConfiancaConfigService;
import com.confApi.chatconfianca.service.ChatConfiancaRequestAuthorizationService;
import com.confApi.exception.RegraDeNegocioException;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MobileChatIdentityTest {
    MobileChatDelegations grants = new MobileChatDelegations();
    MobileChatIdentityFilter filter = new MobileChatIdentityFilter(grants);
    MobileSessions.Session session = new MobileSessions.Session("fixture", "device", MobileUpstream.JSON.createObjectNode().put("id",42), true);
    byte[] body = "{\"codgUsuario\":42,\"texto\":\"olá\"}".getBytes(StandardCharsets.UTF_8);
    String path = "v1/chat-confianca/mensagens";
    @BeforeEach void setup() { session.accessUntil = Instant.now().plusSeconds(600); SecurityContextHolder.clearContext(); }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    String grant() { return grants.issue(session, "POST", path, "codgUsuario=42", body); }
    MockHttpServletRequest request(String token) {
        var request = new MockHttpServletRequest("POST", "/" + path);
        request.setQueryString("codgUsuario=42"); request.setContent(body);
        request.setCharacterEncoding("UTF-8"); request.setContentType("application/json");
        request.addHeader("Authorization", "Bearer technical-token");
        if (token != null) request.addHeader(MobileChatDelegations.HEADER, token);
        return request;
    }
    @Test void validGrantUsesIndividualIdentityAndKeepsAuthorizationChecksAndBody() throws Exception {
        var config = mock(ChatConfiancaConfigService.class);
        var own = new RefUsuario(); own.setLoginUsuario("fixture");
        var other = new RefUsuario(); other.setLoginUsuario("other");
        when(config.buscarUsuarioReferencia(42)).thenReturn(own);
        when(config.buscarUsuarioReferencia(99)).thenReturn(other);
        var authorization = new ChatConfiancaRequestAuthorizationService(config, "api.confplus");
        var invoked = new AtomicBoolean();
        filter.doFilter(request(grant()), new MockHttpServletResponse(), (req, res) -> {
            invoked.set(true);
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertEquals("fixture", auth.getName());
            assertTrue(auth instanceof MobileChatAuthentication);
            assertTrue(((MobileChatAuthentication)auth).owns(42));
            assertFalse(((MobileChatAuthentication)auth).owns(99));
            assertEquals("[ROLE_USER]", auth.getAuthorities().toString());
            authorization.validarUsuario(auth, 42);
            assertThrows(RegraDeNegocioException.class, () -> authorization.validarUsuario(auth, 99));
            assertArrayEquals(body, req.getInputStream().readAllBytes());
            assertEquals("Bearer technical-token", ((javax.servlet.http.HttpServletRequest)req).getHeader("Authorization"));
        });
        assertTrue(invoked.get()); assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    @Test void missingGrantKeepsLegacyAuthentication() throws Exception {
        var invoked = new AtomicBoolean();
        filter.doFilter(request(null), new MockHttpServletResponse(), (req, res) -> invoked.set(true));
        assertTrue(invoked.get());
    }
    @Test void forgedGrantIsRejectedEvenWithTechnicalBearer() throws Exception { denied(request("forged")); }
    @Test void grantCannotBeReplayed() throws Exception {
        String token = grant();
        filter.doFilter(request(token), new MockHttpServletResponse(), (req, res) -> {});
        denied(request(token));
    }
    @Test void alteredMethodPathQueryOrBodyIsRejected() throws Exception {
        for (int i = 0; i < 4; i++) {
            var req = request(grant());
            switch (i) {
                case 0 -> req.setMethod("GET");
                case 1 -> req.setRequestURI("/v1/chat-confianca/conversas");
                case 2 -> req.setQueryString("codgUsuario=99");
                case 3 -> req.setContent("{\"codgUsuario\":99}".getBytes(StandardCharsets.UTF_8));
            }
            denied(req);
        }
    }
    @Test void revokedOrExpiredSessionIsRejected() throws Exception {
        String token = grant(); session.revoked = true; denied(request(token));
        session.revoked = false; token = grant(); session.accessUntil = Instant.EPOCH; denied(request(token));
    }
    @Test void expiredGrantIsRejected() {
        var clock = mock(Clock.class); Instant now = Instant.now(); when(clock.instant()).thenReturn(now);
        var expiring = new MobileChatDelegations(clock);
        String token = expiring.issue(session, "POST", path, null, body);
        when(clock.instant()).thenReturn(now.plusSeconds(31));
        assertThrows(MobileFailure.class, () -> expiring.redeem(token, "POST", "/" + path, null, body));
    }
    @Test void delegationCannotBeCreatedForOtherServicesOrPendingLogin() {
        assertThrows(MobileFailure.class, () -> grants.issue(session, "POST", "v1/aereo/reservar", null, body));
        session.primary = false; assertThrows(MobileFailure.class, this::grant);
    }
    private void denied(MockHttpServletRequest request) throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Unauthorized request reached controller"));
        assertEquals(401, response.getStatus()); assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
