package com.confApi.rodoviario;

import com.confApi.confApp.*;
import com.confApi.config.UrlConfig;
import com.confApi.hub.rodoviario.RodoviarioHubClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.EnumSource;
import com.confApi.rodoviario.dto.PesquisaRodoviarioRequestDTO;
import org.springframework.http.*;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.net.SocketTimeoutException;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RodoviarioHttpClientTest {
    String original, originalManager;
    ObjectMapper json;
    MockRestServiceServer server;
    RodoviarioHttpClient http;
    RodoviarioHubClient hub;
    ConfAppService auth;
    @BeforeEach void setup() {
        original = UrlConfig.URL_CONFIANCA_HUB; UrlConfig.URL_CONFIANCA_HUB = "http://hub.test/SandBox/";
        originalManager = UrlConfig.URL_CONFIANCA_MANAGER; UrlConfig.URL_CONFIANCA_MANAGER = "http://manager.test:8082/";
        json = Jackson2ObjectMapperBuilder.json().build(); var rest = new RestTemplate();
        server = MockRestServiceServer.bindTo(rest).build(); auth = mock(ConfAppService.class);
        var token = new ConfAppResp(); token.setToken("APP_TEST"); when(auth.token()).thenReturn(token);
        http = new RodoviarioHttpClient(rest, auth, json);
        hub = new RodoviarioHubClient(http);
    }
    @AfterEach void cleanup() { UrlConfig.URL_CONFIANCA_HUB = original; UrlConfig.URL_CONFIANCA_MANAGER = originalManager; }
    @Test void timeoutDeCheckoutExigeConsultaSemRepetirRequisicao() throws Exception {
        server.expect(requestTo("http://hub.test/SandBox/api/rodoviario/emitir"))
                .andExpect(header("Authorization", "Bearer APP_TEST"))
                .andRespond(request -> { throw new SocketTimeoutException("segredo-nao-deve-vazar"); });
        var ex = assertThrows(RodoviarioIntegracaoException.class,
                () -> hub.post(RodoviarioHubClient.Operacao.emitir, json.readTree("{}")));
        assertEquals(504, ex.getStatus()); assertTrue(ex.getResposta().path("requerConferencia").asBoolean());
        assertNull(ex.getCause()); assertFalse(ex.getResposta().toString().contains("segredo")); server.verify();
    }
    @ParameterizedTest
    @EnumSource(value = RodoviarioHubClient.Operacao.class, names = {"pesquisa", "pesquisaconexoes", "sugestoesconexao"})
    void serializaRequestTipadoDePesquisaComDataIsoEContratoDoHub(RodoviarioHubClient.Operacao operacao) {
        var request = new PesquisaRodoviarioRequestDTO(); request.setOrigem(101); request.setDestino(202);
        request.setDataViagem(java.time.LocalDate.of(2030, 1, 15));
        server.expect(requestTo("http://hub.test/SandBox/api/rodoviario/" + operacao.name()))
                .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization", "Bearer APP_TEST"))
                .andExpect(jsonPath("$.sistema").value("QueroPassagem"))
                .andExpect(jsonPath("$.origem").value(101))
                .andExpect(jsonPath("$.destino").value(202))
                .andExpect(jsonPath("$.dataViagem").value("2030-01-15"))
                .andExpect(jsonPath("$.rotaValida").doesNotExist())
                .andRespond(withSuccess("{\"sistema\":\"QueroPassagem\",\"dados\":[]}", MediaType.APPLICATION_JSON));
        var response = operacao == RodoviarioHubClient.Operacao.pesquisa ? hub.pesquisar(request)
                : operacao == RodoviarioHubClient.Operacao.pesquisaconexoes ? hub.pesquisarConexoes(request) : hub.sugerirConexoes(request);
        assertTrue(response.path("dados").isArray()); server.verify();
    }
    @Test void preservaErroEstruturadoDoHub() throws Exception {
        String erro = "{\"sistema\":\"QueroPassagem\",\"codigo\":\"RESERVA_EXPIRADA\",\"mensagens\":[\"Refaca a pesquisa\"],\"requerConferencia\":false}";
        server.expect(requestTo("http://hub.test/SandBox/api/rodoviario/emitir"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(erro));
        var ex = assertThrows(RodoviarioIntegracaoException.class, () -> hub.post(RodoviarioHubClient.Operacao.emitir, json.readTree("{}")));
        assertEquals(409, ex.getStatus()); assertEquals(json.readTree(erro), ex.getResposta()); server.verify();
    }
    @Test void falhaDeTransporteDoManagerMostraCausaEDestinoSemExporSegredos() {
        String originalManager = UrlConfig.URL_CONFIANCA_MANAGER;
        UrlConfig.URL_CONFIANCA_MANAGER = "http://manager.test:8082/";
        var logger = (Logger) LoggerFactory.getLogger(RodoviarioHttpClient.class);
        var logs = new ListAppender<ILoggingEvent>(); logs.start(); logger.addAppender(logs);
        MDC.put(RodoviarioRequestLoggingFilter.REQUEST_ID, "transporte-teste");
        try {
            var params = new org.springframework.util.LinkedMultiValueMap<String, String>();
            params.add("filtro", "SEGREDO_QUERY");
            server.expect(requestTo("http://manager.test:8082/reservaRodoviario?filtro=SEGREDO_QUERY"))
                    .andExpect(header("X-Usuario-Token", "SEGREDO_USUARIO"))
                    .andRespond(request -> { throw new java.io.IOException("SEGREDO_EXCECAO",
                            new java.net.ConnectException("SEGREDO_CAUSA")); });
            var ex = assertThrows(RodoviarioIntegracaoException.class,
                    () -> http.trocar(RodoviarioHttpClient.Servico.MANAGER, "reservaRodoviario",
                            HttpMethod.GET, null, params, "SEGREDO_USUARIO", com.fasterxml.jackson.databind.JsonNode.class, false));
            assertEquals(504, ex.getStatus()); assertFalse(ex.getResposta().path("requerConferencia").asBoolean());
            String messages = logs.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
            assertTrue(messages.contains("transporte requestId=transporte-teste servico=MANAGER host=manager.test porta=8082 causa=ConnectException"));
            assertFalse(messages.contains("SEGREDO")); assertFalse(messages.contains("APP_TEST"));
            assertFalse(ex.getResposta().toString().contains("SEGREDO")); assertNull(ex.getCause());
            assertTrue(logs.list.stream().allMatch(event -> event.getThrowableProxy() == null));
            server.verify();
        } finally {
            UrlConfig.URL_CONFIANCA_MANAGER = originalManager;
            logger.detachAppender(logs); MDC.remove(RodoviarioRequestLoggingFilter.REQUEST_ID);
        }
    }
    @Test void consultaDoManagerRecuperaResetUmaVezSemRenovarCredenciaisOuExporMensagem() {
        var logger = (Logger) LoggerFactory.getLogger(RodoviarioHttpClient.class);
        var logs = new ListAppender<ILoggingEvent>(); logs.start(); logger.addAppender(logs);
        try {
            server.expect(requestTo("http://manager.test:8082/reservaRodoviario"))
                    .andExpect(method(HttpMethod.GET)).andExpect(header("X-Usuario-Token", "USUARIO_TEST"))
                    .andRespond(request -> { throw new java.net.SocketException("Connection reset SEGREDO_TOKEN"); });
            server.expect(requestTo("http://manager.test:8082/reservaRodoviario"))
                    .andExpect(method(HttpMethod.GET)).andExpect(header("X-Usuario-Token", "USUARIO_TEST"))
                    .andRespond(withSuccess("{\"content\":[]}", MediaType.APPLICATION_JSON));
            var result = http.trocar(RodoviarioHttpClient.Servico.MANAGER, "reservaRodoviario", HttpMethod.GET,
                    null, null, "USUARIO_TEST", com.fasterxml.jackson.databind.JsonNode.class, false);
            assertTrue(result.path("content").isArray()); verify(auth, times(1)).token(); server.verify();
            String messages = logs.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
            assertTrue(messages.contains("motivo=CONEXAO_REINICIADA tentativa=1"));
            assertFalse(messages.contains("SEGREDO_TOKEN"));
        } finally { logger.detachAppender(logs); }
    }
    @Test void consultaDoManagerInterrompidaDuasVezesRetornaFalhaSemTerceiraTentativa() {
        for (int tentativa = 0; tentativa < 2; tentativa++)
            server.expect(requestTo("http://manager.test:8082/reservaRodoviario"))
                    .andRespond(request -> { throw new java.net.SocketException("Socket closed"); });
        var ex = assertThrows(RodoviarioIntegracaoException.class,
                () -> http.trocar(RodoviarioHttpClient.Servico.MANAGER, "reservaRodoviario", HttpMethod.GET,
                        null, null, "USUARIO_TEST", com.fasterxml.jackson.databind.JsonNode.class, false));
        assertEquals(504, ex.getStatus()); assertFalse(ex.getResposta().path("requerConferencia").asBoolean());
        verify(auth, times(1)).token(); server.verify();
    }
    static java.util.stream.Stream<Arguments> falhasSemRepeticao() {
        var manager = RodoviarioHttpClient.Servico.MANAGER;
        return java.util.stream.Stream.of(
                Arguments.of(manager, HttpMethod.POST, true, new java.net.SocketException("Connection reset")),
                Arguments.of(manager, HttpMethod.PUT, true, new java.net.SocketException("Connection reset")),
                Arguments.of(manager, HttpMethod.DELETE, true, new java.net.SocketException("Connection reset")),
                Arguments.of(RodoviarioHttpClient.Servico.HUB, HttpMethod.POST, true, new java.net.SocketException("Connection reset")),
                Arguments.of(manager, HttpMethod.GET, false, new SocketTimeoutException("Read timed out")),
                Arguments.of(manager, HttpMethod.GET, false, new java.net.ConnectException("Connection refused")),
                Arguments.of(manager, HttpMethod.GET, false, new java.net.NoRouteToHostException("No route")),
                Arguments.of(manager, HttpMethod.GET, true, new java.net.SocketException("Connection reset")));
    }
    @ParameterizedTest @MethodSource("falhasSemRepeticao")
    void naoRepeteMutacoesTimeoutConexaoRecusadaOuOperacaoComEfeito(RodoviarioHttpClient.Servico servico,
            HttpMethod metodo, boolean efeito, java.io.IOException falha) {
        String base = servico == RodoviarioHttpClient.Servico.MANAGER
                ? "http://manager.test:8082/" : "http://hub.test/SandBox/";
        server.expect(requestTo(base + "teste")).andExpect(method(metodo))
                .andRespond(request -> { throw falha; });
        var ex = assertThrows(RodoviarioIntegracaoException.class,
                () -> http.trocar(servico, "teste", metodo, null, null, "USUARIO_TEST",
                        com.fasterxml.jackson.databind.JsonNode.class, efeito));
        assertEquals(504, ex.getStatus()); assertEquals(efeito, ex.getResposta().path("requerConferencia").asBoolean());
        server.verify();
    }
    @Test void htmlDoHubNaoEVistoComoSucessoNemExpostoAoCliente() throws Exception {
        server.expect(requestTo("http://hub.test/SandBox/api/rodoviario/pesquisa"))
                .andRespond(withSuccess("<html>segredo</html>", MediaType.TEXT_HTML));
        var ex = assertThrows(RodoviarioIntegracaoException.class, () -> hub.post(RodoviarioHubClient.Operacao.pesquisa, json.readTree("{}")));
        assertEquals(502, ex.getStatus()); assertFalse(ex.getResposta().path("requerConferencia").asBoolean());
        assertFalse(ex.getResposta().toString().contains("segredo")); server.verify();
    }
    @Test void autenticacaoAusenteImpedeChamadaAoHub() throws Exception {
        when(auth.token()).thenReturn(null);
        var ex = assertThrows(RodoviarioIntegracaoException.class, () -> hub.post(RodoviarioHubClient.Operacao.reservar, json.readTree("{}")));
        assertEquals(503, ex.getStatus()); assertFalse(ex.getResposta().path("requerConferencia").asBoolean()); server.verify();
    }
    @Test void credencialDaAplicacaoRejeitadaPeloHubNaoViraErroDeLoginDoCliente() throws Exception {
        server.expect(requestTo("http://hub.test/SandBox/api/rodoviario/pesquisa"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("segredo"));
        var ex = assertThrows(RodoviarioIntegracaoException.class, () -> hub.post(RodoviarioHubClient.Operacao.pesquisa, json.readTree("{}")));
        assertEquals(502, ex.getStatus()); assertEquals("AUTENTICACAO_HUB_REJEITADA", ex.getResposta().path("codigo").asText()); server.verify();
    }
    @Test void respostaSemEnvelopeNaoEEmitidaComSucesso() throws Exception {
        server.expect(requestTo("http://hub.test/SandBox/api/rodoviario/emitir"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        var ex = assertThrows(RodoviarioIntegracaoException.class, () -> hub.post(RodoviarioHubClient.Operacao.emitir, json.readTree("{}")));
        assertEquals(502, ex.getStatus()); assertTrue(ex.getResposta().path("requerConferencia").asBoolean()); server.verify();
    }
    @Test void logsIdentificamEtapasSemExporCredenciaisCorpoOuErrosDoFornecedor() throws Exception {
        var logger = (Logger) LoggerFactory.getLogger(RodoviarioHttpClient.class);
        var logs = new ListAppender<ILoggingEvent>(); logs.start(); logger.addAppender(logs);
        MDC.put(RodoviarioRequestLoggingFilter.REQUEST_ID, "requisicao-teste");
        try {
            server.expect(requestTo("http://hub.test/SandBox/api/rodoviario/pesquisa"))
                    .andRespond(withSuccess("{\"sistema\":\"QueroPassagem\",\"dados\":[]}", MediaType.APPLICATION_JSON));
            hub.post(RodoviarioHubClient.Operacao.pesquisa, json.readTree("{\"passageiro\":\"SEGREDO_PASSAGEIRO\"}"));
            when(auth.token()).thenThrow(new IllegalStateException("SEGREDO_AUTENTICACAO"));
            assertThrows(RodoviarioIntegracaoException.class, () -> hub.post(RodoviarioHubClient.Operacao.pesquisa, json.readTree("{}")));
            String messages = logs.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
            assertTrue(messages.contains("inicio requestId=requisicao-teste servico=HUB metodo=POST rota=api/rodoviario/pesquisa"));
            assertTrue(messages.contains("autenticacao requestId=requisicao-teste"));
            assertTrue(messages.contains("envio requestId=requisicao-teste"));
            assertTrue(messages.contains("concluida requestId=requisicao-teste"));
            assertTrue(messages.contains("falhou requestId=requisicao-teste servico=HUB status=503"));
            assertFalse(messages.contains("SEGREDO")); assertFalse(messages.contains("APP_TEST"));
            server.verify();
        } finally { logger.detachAppender(logs); MDC.remove(RodoviarioRequestLoggingFilter.REQUEST_ID); }
    }
}
