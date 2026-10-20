package com.confApi.aereo;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

class AntifraudePortalHeadersTest {
    private static final String OP = "db046b30-b647-44f7-aac6-365d540e5c45";
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private void auth(String name) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                name, "ficticio", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
    private MockHttpServletRequest request() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Antifraude-Operacao", OP);
        request.addHeader("X-Antifraude-IP", "203.0.113.20");
        request.addHeader("X-Usuario-Token", "token-humano-ficticio");
        request.addHeader("X-Antifraude-Contexto", "a".repeat(64));
        request.addHeader("X-Antifraude-Sessao", "b".repeat(64));
        request.addHeader("X-Header-Arbitrario", "nao-repassar");
        return request;
    }

    @Test void soEncaminhaCincoCabecalhosDeConsumidorEPeerAutorizados() {
        auth("portal");
        var result = new AntifraudePortalHeaders("portal", "127.0.0.1").extract(request());
        assertEquals(5, result.size());
        assertEquals(OP, result.getFirst("X-Antifraude-Operacao"));
        assertEquals("b".repeat(64), result.getFirst("X-Antifraude-Sessao"));
        assertFalse(result.containsKey("X-Header-Arbitrario"));
        assertFalse(result.containsKey("Authorization"));
    }

    @Test void naoConfiaNaOrigemDeclaradaPorOutroConsumidorOuPeer() {
        var policy = new AntifraudePortalHeaders("portal", "127.0.0.1");
        auth("outro");
        assertThrows(ResponseStatusException.class, () -> policy.extract(request()));
        auth("portal");
        var request = request(); request.setRemoteAddr("203.0.113.50");
        assertThrows(ResponseStatusException.class, () -> policy.extract(request));
        SecurityContextHolder.clearContext();
        assertThrows(ResponseStatusException.class, () -> policy.extract(request()));
    }

    @Test void configuracaoVaziaNaoAutorizaEProtocoloLegadoSemOperacaoNaoInventaGrant() {
        auth("portal");
        assertThrows(ResponseStatusException.class, () -> new AntifraudePortalHeaders("", "").extract(request()));
        assertTrue(new AntifraudePortalHeaders("", "").extract(new MockHttpServletRequest()).isEmpty());
    }

    @Test void validaOsResumosAntesDoEncaminhamento() {
        auth("portal");
        var request = request(); request.removeHeader("X-Antifraude-Sessao");
        request.addHeader("X-Antifraude-Sessao", "valor-nao-canonico");
        assertThrows(ResponseStatusException.class,
                () -> new AntifraudePortalHeaders("portal", "127.0.0.1").extract(request));
    }

    @Test void ipv4MappedReconheceMesmoPeerSemConfiarEmNomeDns() {
        auth("portal");
        var request = request(); request.setRemoteAddr("::ffff:127.0.0.1");
        assertEquals(OP, new AntifraudePortalHeaders("portal", "127.0.0.1").extract(request).getFirst("X-Antifraude-Operacao"));
        assertThrows(IllegalArgumentException.class, () -> new AntifraudePortalHeaders("portal", "localhost"));
    }
}
