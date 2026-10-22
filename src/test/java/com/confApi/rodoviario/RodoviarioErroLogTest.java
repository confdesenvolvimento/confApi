package com.confApi.rodoviario;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class RodoviarioErroLogTest {
    final Logger logger = (Logger) LoggerFactory.getLogger(RodoviarioErroLog.class);
    ListAppender<ILoggingEvent> logs;
    @BeforeEach void setup() {
        logs = new ListAppender<>(); logs.start(); logger.addAppender(logs);
        MDC.put(RodoviarioRequestLoggingFilter.REQUEST_ID, "request-teste");
    }
    @AfterEach void cleanup() { logger.detachAppender(logs); MDC.remove(RodoviarioRequestLoggingFilter.REQUEST_ID); }
    @Test void registraMotivoLocalCodigoEOrigemComIdentificadorDaRequisicao() {
        var ex = RodoviarioIntegracaoException.falha(400, "CONFAPI", "REQUEST_INVALIDO", "Informe os vinculos de origem e destino do Manager.", false);
        RodoviarioErroLog.registrar("controller", ex);
        var event = logs.list.get(0); String message = event.getFormattedMessage();
        assertEquals(Level.WARN, event.getLevel());
        assertTrue(message.contains("requestId=request-teste etapa=controller servico=CONFAPI status=400 codigo=REQUEST_INVALIDO"));
        assertTrue(message.contains("motivo=Informe os vinculos de origem e destino do Manager."));
        assertTrue(message.contains("RodoviarioErroLogTest.java:"));
    }
    @Test void erroDoFornecedorNaoImprimeMensagensTokensOuCorpo() throws Exception {
        var body = new ObjectMapper().readTree("{\"codigo\":\"RESPOSTA_INVALIDA_FORNECEDOR\",\"mensagens\":[\"SEGREDO_TOKEN CPF_CARTAO\"],\"requerConferencia\":true}");
        RodoviarioErroLog.registrar("resultado-parcial", new RodoviarioIntegracaoException(502, body));
        var event = logs.list.get(0); String message = event.getFormattedMessage();
        assertEquals(Level.ERROR, event.getLevel());
        assertTrue(message.contains("codigo=RESPOSTA_INVALIDA_FORNECEDOR"));
        assertTrue(message.contains("etapa=resultado-parcial")); assertTrue(message.contains("requerConferencia=true"));
        assertFalse(message.contains("SEGREDO")); assertFalse(message.contains("CPF_CARTAO"));
        assertNull(event.getThrowableProxy());
    }
    @Test void excecaoInesperadaMostraTipoELocalSemMensagemOuCausasArbitrarias() {
        RodoviarioErroLog.registrar("controller", new IllegalStateException("SEGREDO_TOKEN", new RuntimeException("SEGREDO_CARTAO")));
        var event = logs.list.get(0); String message = event.getFormattedMessage();
        assertEquals(Level.ERROR, event.getLevel()); assertTrue(message.contains("status=500 codigo=ERRO_INTERNO tipo=IllegalStateException"));
        assertTrue(message.contains("RodoviarioErroLogTest.java:")); assertFalse(message.contains("SEGREDO"));
        assertNull(event.getThrowableProxy());
    }
    @Test void reasonDoResponseStatusNaoVazaParaOLog() {
        RodoviarioErroLog.registrar("controller", new ResponseStatusException(HttpStatus.BAD_REQUEST, "SEGREDO_CPF"));
        assertTrue(logs.list.get(0).getFormattedMessage().contains("status=400"));
        assertFalse(logs.list.get(0).getFormattedMessage().contains("SEGREDO"));
    }
    @Test void preservaCodigoDeVinculosInvalidosNoDiagnostico() {
        RodoviarioErroLog.registrar("resultado-parcial", RodoviarioIntegracaoException.falha(400, "CONFAPI",
                "VINCULOS_DESTINO_INVALIDOS", "Vinculos ausentes: [9331].", true));
        assertTrue(logs.list.get(0).getFormattedMessage().contains("codigo=VINCULOS_DESTINO_INVALIDOS"));
    }
}
