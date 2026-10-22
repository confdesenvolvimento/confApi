package com.confApi.rodoviario;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import javax.servlet.ServletException;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class RodoviarioRequestLoggingFilterTest {
    final RodoviarioRequestLoggingFilter filter = new RodoviarioRequestLoggingFilter();
    final Logger logger = (Logger) LoggerFactory.getLogger(RodoviarioRequestLoggingFilter.class);
    ListAppender<ILoggingEvent> logs;
    @BeforeEach void setup() { logs = new ListAppender<>(); logs.start(); logger.addAppender(logs); }
    @AfterEach void cleanup() { logger.detachAppender(logs); MDC.remove(RodoviarioRequestLoggingFilter.REQUEST_ID); }
    private String messages() { return logs.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n")); }

    @Test void registraAcessoRejeitadoAntesDoControllerSemTokensQueryOuCorpo() throws Exception {
        var request = new MockHttpServletRequest("GET", "/v1/rodoviario/viacoes");
        request.setQueryString("sistema=QueroPassagem&token=SEGREDO_QUERY");
        request.addHeader("Authorization", "Bearer SEGREDO_HEADER");
        request.setContent("SEGREDO_CORPO".getBytes());
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {
            assertEquals(response.getHeader(RodoviarioRequestLoggingFilter.HEADER), MDC.get(RodoviarioRequestLoggingFilter.REQUEST_ID));
            ((javax.servlet.http.HttpServletResponse) res).setStatus(403);
        });
        String id = response.getHeader(RodoviarioRequestLoggingFilter.HEADER);
        assertNotNull(id); assertEquals(36, id.length());
        assertEquals(2, logs.list.size());
        assertTrue(messages().contains("entrada requestId=" + id + " metodo=GET rota=/v1/rodoviario/viacoes"));
        assertTrue(messages().contains("saida requestId=" + id + " status=403 duracaoMs="));
        assertFalse(messages().contains("SEGREDO")); assertNull(MDC.get(RodoviarioRequestLoggingFilter.REQUEST_ID));
        assertTrue(filter.getClass().getAnnotation(Order.class).value() < -100, "Deve executar antes do filtro de seguranca");
    }
    @Test void reconheceContextoDoWarEAliasSemConfiarEmIdDoCliente() throws Exception {
        var request = new MockHttpServletRequest("POST", "/ConfApi/rodoviario/pesquisar");
        request.setContextPath("/ConfApi"); request.addHeader(RodoviarioRequestLoggingFilter.HEADER, "NAO_CONFIAVEL");
        var response = new MockHttpServletResponse();
        MDC.put(RodoviarioRequestLoggingFilter.REQUEST_ID, "id-anterior");
        filter.doFilter(request, response, (req, res) -> {});
        assertTrue(messages().contains("rota=/rodoviario/pesquisar"));
        assertFalse(messages().contains("NAO_CONFIAVEL"));
        assertNotEquals("NAO_CONFIAVEL", response.getHeader(RodoviarioRequestLoggingFilter.HEADER));
        assertEquals("id-anterior", MDC.get(RodoviarioRequestLoggingFilter.REQUEST_ID));
    }
    @Test void ignoraRotasDeOutrosModulos() throws Exception {
        for (String path : new String[]{"/api/usuario/auth", "/rodoviarioOutro/viacoes", "/v1/rodoviarios"}) {
            var response = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest("GET", path), response, (req, res) -> {});
            assertNull(response.getHeader(RodoviarioRequestLoggingFilter.HEADER));
        }
        assertTrue(logs.list.isEmpty());
    }
    @Test void registraFalhaInesperadaERestauraContextoSemExporMensagem() {
        var response = new MockHttpServletResponse();
        assertThrows(ServletException.class, () -> filter.doFilter(new MockHttpServletRequest("GET", "/rodoviario/viacoes"), response,
                (req, res) -> { throw new ServletException("SEGREDO_EXCECAO"); }));
        assertTrue(messages().contains("status=500")); assertFalse(messages().contains("SEGREDO"));
        assertNull(MDC.get(RodoviarioRequestLoggingFilter.REQUEST_ID));
    }
    @Test void evitaQuebraDeLinhaEQueryNoCampoRota() {
        assertEquals("api/rodoviario/viacoes", RodoviarioRequestLoggingFilter.safe("api/rodoviario/viacoes?email=SEGREDO"));
        assertEquals("/rodoviario/_texto", RodoviarioRequestLoggingFilter.safe("/rodoviario/\ntexto"));
        assertEquals(200, RodoviarioRequestLoggingFilter.safe("a".repeat(300)).length());
    }
}