package com.confApi.rodoviario;

import com.confApi.confApp.*;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.rodoviario.RodoviarioManagerClient;
import com.confApi.db.confManager.rodoviario.dto.ReservaRodoviarioDetalheDTO;
import com.confApi.hub.rodoviario.RodoviarioHubClient;
import com.confApi.rodoviario.dto.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.client.*;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RodoviarioReservaServiceTest {
    static final String HUB = "http://hub.test/SandBox/";
    static final String MANAGER = "http://manager.test/confiancamanger2/";
    static final String COMPRA = """
        {"codigoFornecedor":123456,"sistema":"QueroPassagem","situacao":"paid",
         "pagamento":{"total":22.40,"bilhetes":18.67,"taxas":3.73,"seguro":0,"pedagio":0},
         "trechos":[{"origem":{"codigoFornecedor":"ROD_1"},"destino":{"codigoFornecedor":"CIT_15"},
          "viacao":{"nome":"Viacao Teste"},"partida":"2030-01-15T19:00:00","chegada":"2030-01-16T03:00:00",
          "passageiros":[{"codigoFornecedor":321,"nome":"Maria Silva","tipoDocumento":"CPF","documento":"12345678909",
           "dataNascimento":"2000-01-01","poltrona":"01","bilhete":{"numero":"ABC123","situacao":"emitted"}}]}]}
        """;
    static final String DETALHE = """
        {"reserva":{"codgReservaRodoviario":10,"codgAgencia":7,"codgSistema":9,"localizador":"123456","status":1},
         "trechos":[{"codgRodoviarioTrecho":100,"codgReservaRodoviario":10,"codgCidadeOrigem":11,"codgCidadeDestino":22}],
         "passageiros":[{"codgPassageiro":200,"codgRodoviarioTrecho":100,"nomePassageiro":"Maria","sobrenomePassageiro":"Silva","sexo":1}],
         "assentos":[{"codgRodoviarioAssento":300,"codgRodoviarioTrecho":100,"codgRodoviarioPassageiro":200,"numeroAssento":"01"}],
         "bilhetes":[{"codgRodoviarioBilhete":400,"codgRodoviarioTrecho":100,"codgRodoviarioPassageiro":200,"numerBilhete":"ABC123","status":1}],
         "valores":{"valorTotalNet":22.40,"valorTotalMkp":22.40}}
        """;
    static final String REQUEST = """
        {"reservaRodoviario":{"viagens":[{"codigoViagem":"travel-1","passageiros":[{"nome":"Maria Silva","documento":"12345678909","tipoDocumento":"CPF","poltrona":"01"}]}]},
         "emissao":{"email":"teste@example.com","pagamento":{"metodo":"preApproved","parcelas":1}},
         "dadosManager":{"chaveIdempotencia":"pedido-rodoviario-001","reserva":{"codgAgencia":7,"codgSistema":9,"codgUsuarioCriacao":7},
          "valores":{"valorTotalMkp":22.40},
          "trechos":[{"trecho":{"codgCidadeOrigem":101,"codgCidadeDestino":202},
           "passageiros":[{"passageiro":{"nomePassageiro":"Maria","sobrenomePassageiro":"Silva","sexo":1,"cpf":"12345678909"}}]}]}}
        """;
    ObjectMapper json;
    MockRestServiceServer server;
    RodoviarioReservaService service;
    RodoviarioHubClient hub;
    RodoviarioManagerClient manager;
    String oldHub, oldManager;

    @BeforeEach void setup() {
        oldHub = UrlConfig.URL_CONFIANCA_HUB; oldManager = UrlConfig.URL_CONFIANCA_MANAGER;
        UrlConfig.URL_CONFIANCA_HUB = HUB; UrlConfig.URL_CONFIANCA_MANAGER = MANAGER;
        json = Jackson2ObjectMapperBuilder.json().featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
        var rest = new RestTemplate(); server = MockRestServiceServer.bindTo(rest).build();
        var auth = mock(ConfAppService.class); var token = new ConfAppResp(); token.setToken("APP_TEST"); when(auth.token()).thenReturn(token);
        var http = new RodoviarioHttpClient(rest, auth, json); hub = new RodoviarioHubClient(http); manager = new RodoviarioManagerClient(http);
        service = new RodoviarioReservaService(hub, manager, new RodoviarioReservaMapper(json), json);
    }
    @AfterEach void cleanup() { UrlConfig.URL_CONFIANCA_HUB = oldHub; UrlConfig.URL_CONFIANCA_MANAGER = oldManager; }
    RodoviarioCompraModel request() throws Exception { return json.readValue(REQUEST, RodoviarioCompraModel.class); }
    String envelope(String compra) { return "{\"sistema\":\"QueroPassagem\",\"dados\":" + compra + "}"; }
    ResponseActions expectManager(String rota, HttpMethod metodo) {
        return server.expect(requestTo(MANAGER + rota)).andExpect(method(metodo))
                .andExpect(header("Authorization", "Bearer APP_TEST")).andExpect(header("X-Usuario-Token", "USUARIO_TEST"));
    }
    ResponseActions expectHub(String rota) {
        return server.expect(requestTo(HUB + "api/rodoviario/" + rota)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer APP_TEST")).andExpect(headerDoesNotExist("X-Usuario-Token"));
    }
    void destinos() {
        destinosCidade(101, "{\"codgVinculoCidadeSistema\":11,\"codgCidade\":101,\"codgDestinoSistema\":\"ROD_1\"}");
        destinosCidade(202, "{\"codgVinculoCidadeSistema\":22,\"codgCidade\":202,\"codgDestinoSistema\":\"CIT_15\"}");
    }
    void destinosCidade(int cidade, String registros) {
        expectManager("rodoviario/destinos?codgSistema=9&codgCidade=" + cidade + "&page=0&size=100", HttpMethod.GET)
                .andRespond(withSuccess("{\"content\":[" + registros + "],\"last\":true}", MediaType.APPLICATION_JSON));
    }
    void destinosSalvos() {
        expectManager("rodoviario/destinos?codgSistema=9&page=0&size=100", HttpMethod.GET)
                .andRespond(withSuccess("{\"content\":[{\"codgVinculoCidadeSistema\":11,\"codgDestinoSistema\":\"ROD_1\"},{\"codgVinculoCidadeSistema\":22,\"codgDestinoSistema\":\"CIT_15\"}],\"last\":true}", MediaType.APPLICATION_JSON));
    }
    @Test void reservaCompletaUsaSomenteBearerDaAplicacaoNoManager() throws Exception {
        novoCheckout(COMPRA);
        server.expect(requestTo(MANAGER + "reservaRodoviario/findByLocalizador/123456?codgSistema=9"))
                .andExpect(header("Authorization", "Bearer APP_TEST")).andExpect(headerDoesNotExist("X-Usuario-Token"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo(MANAGER + "rodoviario/destinos?codgSistema=9&codgCidade=101&page=0&size=100"))
                .andExpect(header("Authorization", "Bearer APP_TEST")).andExpect(headerDoesNotExist("X-Usuario-Token"))
                .andRespond(withSuccess("{\"content\":[{\"codgVinculoCidadeSistema\":11,\"codgCidade\":101,\"codgDestinoSistema\":\"ROD_1\"}],\"last\":true}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(MANAGER + "rodoviario/destinos?codgSistema=9&codgCidade=202&page=0&size=100"))
                .andExpect(header("Authorization", "Bearer APP_TEST")).andExpect(headerDoesNotExist("X-Usuario-Token"))
                .andRespond(withSuccess("{\"content\":[{\"codgVinculoCidadeSistema\":22,\"codgCidade\":202,\"codgDestinoSistema\":\"CIT_15\"}],\"last\":true}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(MANAGER + "reservaRodoviario/salvarCompleta"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization", "Bearer APP_TEST"))
                .andExpect(headerDoesNotExist("X-Usuario-Token"))
                .andRespond(withSuccess(DETALHE, MediaType.APPLICATION_JSON));
        server.expect(requestTo(MANAGER + "reservaRodoviario/detalhe/10"))
                .andExpect(header("Authorization", "Bearer APP_TEST")).andExpect(headerDoesNotExist("X-Usuario-Token"))
                .andRespond(withSuccess(DETALHE, MediaType.APPLICATION_JSON));
        var result=service.reservar(request(), null);
        assertTrue(result.isSuccess());assertFalse(result.isRequerConferencia());
        assertEquals(10,result.getReserva().getReserva().getCodgReservaRodoviario());server.verify();
    }
    void novoCheckout(String compra) {
        expectHub("consultar").andExpect(jsonPath("$.referenciaExterna").value(org.hamcrest.Matchers.startsWith("ROD-")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        expectHub("reservar").andExpect(jsonPath("$.viagens[0].codigoViagem").value("travel-1"))
                .andRespond(withSuccess(envelope("{\"codigoFornecedor\":\"booking-1\"}"), MediaType.APPLICATION_JSON));
        expectHub("emitir").andExpect(jsonPath("$.codigoReserva").value("booking-1"))
                .andExpect(jsonPath("$.referenciaExterna").value(org.hamcrest.Matchers.startsWith("ROD-")))
                .andRespond(withSuccess(envelope(compra), MediaType.APPLICATION_JSON));
        expectHub("consultar").andExpect(jsonPath("$.codigoCompra").value(123456))
                .andRespond(withSuccess(envelope(compra), MediaType.APPLICATION_JSON));
    }
    void semReserva() {
        expectManager("reservaRodoviario/findByLocalizador/123456?codgSistema=9", HttpMethod.GET).andRespond(withStatus(HttpStatus.NOT_FOUND));
        destinos();
    }
    @Test void reservaConsultaGravaERetornaDetalheDoManager() throws Exception {
        novoCheckout(COMPRA); semReserva();
        expectManager("reservaRodoviario/salvarCompleta", HttpMethod.POST)
                .andExpect(jsonPath("$.reserva.localizador").value("123456"))
                .andExpect(jsonPath("$.reserva.codgUsuarioCriacao").value(7))
                .andExpect(jsonPath("$.reserva.status").value(1))
                .andExpect(jsonPath("$.valores.valorTotalNet").value(22.40))
                .andExpect(jsonPath("$.trechos[0].trecho.dataPartida").value("2030-01-15"))
                .andExpect(jsonPath("$.trechos[0].trecho.codgCidadeOrigem").value(11))
                .andExpect(jsonPath("$.trechos[0].trecho.codgCidadeDestino").value(22))
                .andExpect(jsonPath("$.trechos[0].passageiros[0].bilhetes[0].numerBilhete").value("ABC123"))
                .andRespond(withSuccess(DETALHE, MediaType.APPLICATION_JSON));
        expectManager("reservaRodoviario/detalhe/10", HttpMethod.GET).andRespond(withSuccess(DETALHE, MediaType.APPLICATION_JSON));
        var result = service.reservar(request(), "USUARIO_TEST");
        assertTrue(result.isSuccess()); assertFalse(result.isRequerConferencia());
        assertEquals(10, result.getReserva().getReserva().getCodgReservaRodoviario()); assertTrue(result.isMergeExecutado()); server.verify();
    }
    @Test void compraPendenteNaoViraReservaEmitida() throws Exception {
        String pendente = COMPRA.replace("\"paid\"", "\"in_confirmation_process\"");
        String rascunho = DETALHE.replace("\"status\":1", "\"status\":0");
        novoCheckout(pendente); semReserva();
        expectManager("reservaRodoviario/salvarCompleta", HttpMethod.POST).andExpect(jsonPath("$.reserva.status").value(0))
                .andRespond(withSuccess(rascunho, MediaType.APPLICATION_JSON));
        expectManager("reservaRodoviario/detalhe/10", HttpMethod.GET).andRespond(withSuccess(rascunho, MediaType.APPLICATION_JSON));
        var result = service.reservar(request(), "USUARIO_TEST");
        assertTrue(result.isSuccess()); assertEquals(0, result.getReserva().getReserva().getStatus()); assertFalse(result.isStatusAtualizado()); server.verify();
    }
    @Test void reservaSemUsuarioCriadorERecusadaAntesDeQualquerChamadaExterna() throws Exception {
        var req=request();req.getDadosManager().getReserva().setCodgUsuarioCriacao(null);
        var erro=assertThrows(RodoviarioIntegracaoException.class,()->service.reservar(req,null));
        assertEquals(400,erro.getStatus());assertTrue(erro.getDiagnosticoLocal().contains("codgUsuarioCriacao"));
        server.verify();
    }
    @Test void falhaNoManagerAposEmissaoExigeConferenciaSemCancelarOuEmitirNovamente() throws Exception {
        novoCheckout(COMPRA); semReserva();
        expectManager("reservaRodoviario/salvarCompleta", HttpMethod.POST).andRespond(withServerError());
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(RodoviarioErroLog.class);
        var logs = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        logs.start(); logger.addAppender(logs);
        try {
        var result = service.reservar(request(), "USUARIO_TEST");
        assertFalse(result.isSuccess()); assertTrue(result.isRequerConferencia());
        assertEquals(123456, result.getFornecedor().path("dados").path("codigoFornecedor").asInt());
        assertEquals("booking-1", result.getCodigoReservaTemporaria()); server.verify();
        assertTrue(result.getMensagem().contains("nao foi possivel concluir o registro no Manager"));
        org.junit.jupiter.api.Assertions.assertTrue(logs.list.stream().anyMatch(event -> event.getFormattedMessage().contains("etapa=resultado-parcial servico=MANAGER status=500 codigo=ERRO_MANAGER")));
        } finally { logger.detachAppender(logs); }
    }
    @Test void recuperaPorReferenciaSemNovoBookingOuCheckout() throws Exception {
        expectHub("consultar").andRespond(withSuccess(envelope(COMPRA), MediaType.APPLICATION_JSON));
        expectManager("reservaRodoviario/findByLocalizador/123456?codgSistema=9", HttpMethod.GET).andRespond(withSuccess(DETALHE, MediaType.APPLICATION_JSON));
        var result = service.reconciliar(request(), "USUARIO_TEST"); assertTrue(result.isSuccess()); server.verify();
    }
    @Test void recuperacaoSegueParaGravacaoAposSocketInterrompidoE404DoManager() throws Exception {
        expectHub("consultar").andRespond(withSuccess(envelope(COMPRA), MediaType.APPLICATION_JSON));
        expectManager("reservaRodoviario/findByLocalizador/123456?codgSistema=9", HttpMethod.GET)
                .andRespond(request -> { throw new java.net.SocketException("Connection reset"); });
        semReserva();
        expectManager("reservaRodoviario/salvarCompleta", HttpMethod.POST)
                .andExpect(jsonPath("$.reserva.localizador").value("123456"))
                .andRespond(withSuccess(DETALHE, MediaType.APPLICATION_JSON));
        expectManager("reservaRodoviario/detalhe/10", HttpMethod.GET).andRespond(withSuccess(DETALHE, MediaType.APPLICATION_JSON));
        var result = service.reconciliar(request(), "USUARIO_TEST");
        assertTrue(result.isSuccess()); assertFalse(result.isRequerConferencia());
        assertEquals(10, result.getReserva().getReserva().getCodgReservaRodoviario()); server.verify();
    }
    @Test void autenticacaoDaAplicacaoRecusadaPreservaCompraParaRecuperacao() throws Exception {
        novoCheckout(COMPRA);
        expectManager("reservaRodoviario/findByLocalizador/123456?codgSistema=9", HttpMethod.GET)
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        var result = service.reservar(request(), "USUARIO_TEST");
        assertFalse(result.isSuccess()); assertTrue(result.isRequerConferencia());
        assertEquals("AUTENTICACAO_MANAGER_REJEITADA", result.getCodigo());
        assertTrue(result.getMensagem().contains("autenticacao da aplicacao"));
        assertTrue(result.getMensagem().contains("sem repetir reserva ou emissao"));
        assertEquals("booking-1", result.getCodigoReservaTemporaria());
        assertEquals(123456, result.getFornecedor().path("dados").path("codigoFornecedor").asInt());
        assertNull(result.getReserva()); server.verify();
    }
    @Test void recuperaCompraPendenteAposRenovarTokenSemReservarOuEmitirNovamente() throws Exception {
        String pendente = COMPRA.replace("\"paid\"", "\"in_confirmation_process\"");
        String rascunho = DETALHE.replace("\"status\":1", "\"status\":0");
        expectHub("consultar").andRespond(withSuccess(envelope(pendente), MediaType.APPLICATION_JSON));
        semReserva();
        expectManager("reservaRodoviario/salvarCompleta", HttpMethod.POST)
                .andExpect(jsonPath("$.reserva.status").value(0))
                .andRespond(withSuccess(rascunho, MediaType.APPLICATION_JSON));
        expectManager("reservaRodoviario/detalhe/10", HttpMethod.GET).andRespond(withSuccess(rascunho, MediaType.APPLICATION_JSON));
        var result = service.reconciliar(request(), "USUARIO_TEST");
        assertTrue(result.isSuccess()); assertFalse(result.isRequerConferencia());
        assertEquals(0, result.getReserva().getReserva().getStatus());
        server.verify();
    }
    @Test void reconciliacaoNuncaCriaCompraSeReferenciaNaoExiste() throws Exception {
        expectHub("consultar").andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertEquals(409, assertThrows(RodoviarioIntegracaoException.class, () -> service.reconciliar(request(), "USUARIO_TEST")).getStatus()); server.verify();
    }
    @Test void logDoRequestMostraSistemaEVinculosPorTrechoSemDadosPessoaisOuToken() throws Exception {
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(RodoviarioReservaService.class);
        var logs = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        logs.start(); logger.addAppender(logs);
        org.slf4j.MDC.put(RodoviarioRequestLoggingFilter.REQUEST_ID, "request-vinculos");
        try {
            var req = request();
            var volta = json.convertValue(req.getDadosManager().getTrechos().get(0),
                    com.confApi.db.confManager.rodoviario.dto.SalvarReservaRodoviarioRequest.TrechoCompleto.class);
            volta.getTrecho().setCodgCidadeOrigem(202); volta.getTrecho().setCodgCidadeDestino(101);
            req.getDadosManager().getTrechos().add(volta);
            expectHub("consultar").andRespond(withStatus(HttpStatus.NOT_FOUND));
            assertThrows(RodoviarioIntegracaoException.class, () -> service.reconciliar(req, "USUARIO_TEST"));
            var mensagens = logs.list.stream().map(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage)
                    .collect(java.util.stream.Collectors.joining("\n"));
            assertTrue(mensagens.contains("requestId=request-vinculos codgSistema=9 trecho=1 codgCidadeOrigem=101 codgCidadeDestino=202"));
            assertTrue(mensagens.contains("requestId=request-vinculos codgSistema=9 trecho=2 codgCidadeOrigem=202 codgCidadeDestino=101"));
            for (String segredo : new String[]{"USUARIO_TEST", "APP_TEST", "Maria", "12345678909", "teste@example.com"})
                assertFalse(mensagens.contains(segredo));
            server.verify();
        } finally {
            logger.detachAppender(logs); org.slf4j.MDC.remove(RodoviarioRequestLoggingFilter.REQUEST_ID);
        }
    }
    @Test void managerIndisponivelSomenteEConsultadoAposCompraNoHub() throws Exception {
        novoCheckout(COMPRA);
        expectManager("reservaRodoviario/findByLocalizador/123456?codgSistema=9", HttpMethod.GET)
                .andRespond(request -> { throw new java.net.ConnectException("Manager indisponivel"); });
        var result = service.reservar(request(), "USUARIO_TEST");
        assertFalse(result.isSuccess()); assertTrue(result.isRequerConferencia());
        assertEquals("FALHA_COMUNICACAO", result.getCodigo());
        assertEquals("booking-1", result.getCodigoReservaTemporaria());
        assertEquals(123456, result.getFornecedor().path("dados").path("codigoFornecedor").asInt());
        assertTrue(result.getMensagem().contains("registro no Manager")); server.verify();
    }
    @Test void divergenciaDePassageiroNaoGravaCompraNoManager() throws Exception {
        novoCheckout(COMPRA.replace("Maria Silva", "Outra Pessoa")); semReserva();
        var result = service.reservar(request(), "USUARIO_TEST"); assertFalse(result.isSuccess()); assertTrue(result.isRequerConferencia()); server.verify();
    }
    @Test void recuperacaoComVinculosDivergentesRetornaMotivoRealSemGravarOuEmitirNovamente() throws Exception {
        String compra = COMPRA.replace("\"codigoFornecedor\":\"ROD_1\"", "\"codigoFornecedor\":\"ROD_2340\"")
                .replace("\"codigoFornecedor\":\"CIT_15\"", "\"codigoFornecedor\":\"ROD_1\"");
        expectHub("consultar").andRespond(withSuccess(envelope(compra), MediaType.APPLICATION_JSON));
        semReserva();
        var result = service.reconciliar(request(), "USUARIO_TEST");
        assertFalse(result.isSuccess()); assertTrue(result.isRequerConferencia());
        assertEquals("CONFERENCIA_NECESSARIA", result.getCodigo());
        assertTrue(result.getMensagem().contains("Origem/destino do fornecedor divergem"));
        assertTrue(result.getMensagem().contains("Cidade de origem 101"));
        assertTrue(result.getMensagem().contains("cidade de destino 202"));
        assertTrue(result.getMensagem().contains("payload de recuperacao"));
        assertNull(result.getReserva()); server.verify();
    }
    @Test void recuperacaoComCidadeSemDestinoAtivoExplicaCadastroSemGravarOuEmitirNovamente() throws Exception {
        var req = request();
        req.getDadosManager().getTrechos().get(0).getTrecho().setCodgCidadeOrigem(319);
        req.getDadosManager().getTrechos().get(0).getTrecho().setCodgCidadeDestino(9331);
        expectHub("consultar").andRespond(withSuccess(envelope(COMPRA), MediaType.APPLICATION_JSON));
        expectManager("reservaRodoviario/findByLocalizador/123456?codgSistema=9", HttpMethod.GET)
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        destinosCidade(319, "");
        var result = service.reconciliar(req, "USUARIO_TEST");
        assertFalse(result.isSuccess()); assertTrue(result.isRequerConferencia());
        assertEquals("VINCULOS_DESTINO_INVALIDOS", result.getCodigo());
        assertTrue(result.getMensagem().contains("codgCidade 319 no sistema 9"));
        assertTrue(result.getMensagem().contains("ID da tabela cidade"));
        assertTrue(result.getMensagem().contains("payload de recuperacao"));
        assertEquals(123456, result.getFornecedor().path("dados").path("codigoFornecedor").asInt());
        assertNull(result.getReserva());
        for (String segredo : new String[]{"USUARIO_TEST", "APP_TEST", "Maria", "12345678909", "teste@example.com"})
            assertFalse(result.getMensagem().contains(segredo));
        server.verify();
    }
    @Test void reservaRecusadaPeloHubRetornaErroSemEmitirOuAcessarManager() throws Exception {
        expectHub("consultar").andRespond(withStatus(HttpStatus.NOT_FOUND));
        expectHub("reservar").andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                .body("{\"sistema\":\"QueroPassagem\",\"codigo\":\"RESERVA_EXPIRADA\",\"mensagens\":[\"Viagem indisponivel\"],\"requerConferencia\":false}"));
        var result = service.reservar(request(), "USUARIO_TEST");
        assertFalse(result.isSuccess()); assertFalse(result.isRequerConferencia());
        assertEquals("RESERVA_EXPIRADA", result.getCodigo());
        assertEquals("Nao foi possivel realizar a reserva no Hub.", result.getMensagem());
        assertNull(result.getReserva()); assertNull(result.getCodigoReservaTemporaria());
        server.verify();
    }
    @Test void timeoutNaReservaDoHubNaoAcessaManagerNemAfirmaQueNaoHouveReserva() throws Exception {
        expectHub("consultar").andRespond(withStatus(HttpStatus.NOT_FOUND));
        expectHub("reservar").andRespond(request -> { throw new java.net.SocketTimeoutException("timeout"); });
        var result = service.reservar(request(), "USUARIO_TEST");
        assertFalse(result.isSuccess()); assertTrue(result.isRequerConferencia());
        assertEquals("FALHA_COMUNICACAO", result.getCodigo());
        assertTrue(result.getMensagem().contains("Nao foi possivel confirmar a reserva/emissao no Hub"));
        assertNull(result.getReserva()); server.verify();
    }
    @Test void emissaoRecusadaPeloHubNaoGravaReservaTemporariaNoManager() throws Exception {
        expectHub("consultar").andRespond(withStatus(HttpStatus.NOT_FOUND));
        expectHub("reservar").andRespond(withSuccess(envelope("{\"codigoFornecedor\":\"booking-1\"}"), MediaType.APPLICATION_JSON));
        expectHub("emitir").andRespond(withStatus(HttpStatus.CONFLICT));
        var result = service.reservar(request(), "USUARIO_TEST");
        assertFalse(result.isSuccess()); assertTrue(result.isRequerConferencia());
        assertEquals("booking-1", result.getCodigoReservaTemporaria());
        assertNull(result.getReserva()); server.verify();
    }
    @Test void consultaDaCompraAposEmissaoFalhaSemAcessarManager() throws Exception {
        expectHub("consultar").andRespond(withStatus(HttpStatus.NOT_FOUND));
        expectHub("reservar").andRespond(withSuccess(envelope("{\"codigoFornecedor\":\"booking-1\"}"), MediaType.APPLICATION_JSON));
        expectHub("emitir").andRespond(withSuccess(envelope(COMPRA), MediaType.APPLICATION_JSON));
        expectHub("consultar").andRespond(withServerError());
        var result = service.reservar(request(), "USUARIO_TEST");
        assertFalse(result.isSuccess()); assertTrue(result.isRequerConferencia());
        assertTrue(result.getMensagem().contains("no Hub")); assertFalse(result.getMensagem().contains("registro no Manager"));
        assertEquals(123456, result.getFornecedor().path("dados").path("codigoFornecedor").asInt());
        assertNull(result.getReserva()); server.verify();
    }
    RodoviarioConsultaRequest consulta() throws Exception {
        var req = new RodoviarioConsultaRequest(); req.setCodgReservaRodoviario(10);
        req.setConsulta(json.readTree("{\"email\":\"teste@example.com\",\"codigoCompra\":123456}")); req.setMotivo("Solicitacao do passageiro"); return req;
    }
    @Test void cancelamentoTotalConfirmadoAtualizaManager() throws Exception {
        String cancelada = COMPRA.replace("\"paid\"", "\"canceled\"").replace("\"emitted\"", "\"canceled\"");
        expectManager("reservaRodoviario/detalhe/10", HttpMethod.GET).andRespond(withSuccess(DETALHE, MediaType.APPLICATION_JSON));
        expectHub("consultar").andRespond(withSuccess(envelope(COMPRA), MediaType.APPLICATION_JSON));
        expectHub("cancelarcompra").andRespond(withSuccess(envelope(cancelada), MediaType.APPLICATION_JSON));
        expectManager("reservaRodoviario/10/statusFornecedor", HttpMethod.PUT).andExpect(jsonPath("$.status").value(2))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        expectManager("reservaRodoviario/detalhe/10", HttpMethod.GET).andRespond(withSuccess(DETALHE.replace("\"status\":1", "\"status\":2"), MediaType.APPLICATION_JSON));
        var result = service.cancelarReserva(consulta(), "USUARIO_TEST", false);
        assertTrue(result.isSuccess()); assertTrue(result.isStatusAtualizado()); assertEquals(2, result.getReserva().getReserva().getStatus()); server.verify();
    }
    @Test void cancelamentoParcialNaoCancelaReservaInteiraNoManager() throws Exception {
        expectManager("reservaRodoviario/detalhe/10", HttpMethod.GET).andRespond(withSuccess(DETALHE, MediaType.APPLICATION_JSON));
        expectHub("consultar").andRespond(withSuccess(envelope(COMPRA), MediaType.APPLICATION_JSON));
        expectHub("cancelarcompra").andRespond(withSuccess(envelope(COMPRA.replace("\"paid\"", "\"partially_canceled\"")), MediaType.APPLICATION_JSON));
        var result = service.cancelarReserva(consulta(), "USUARIO_TEST", false);
        assertTrue(result.isSuccess()); assertTrue(result.isRequerConferencia()); assertFalse(result.isStatusAtualizado()); server.verify();
    }
    @Test void consultaConfirmaBilhetesPendentesAntesDeConfirmarReserva() throws Exception {
        String rascunho = DETALHE.replace("\"status\":1", "\"status\":0");
        expectManager("reservaRodoviario/detalhe/10", HttpMethod.GET).andRespond(withSuccess(rascunho, MediaType.APPLICATION_JSON));
        expectHub("consultar").andRespond(withSuccess(envelope(COMPRA), MediaType.APPLICATION_JSON)); destinosSalvos();
        expectManager("rodoviarioBilhete/400", HttpMethod.PUT).andExpect(jsonPath("$.status").value(1)).andExpect(jsonPath("$.numerBilhete").value("ABC123"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        expectManager("reservaRodoviario/10/statusFornecedor", HttpMethod.PUT).andExpect(jsonPath("$.status").value(1)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        expectManager("reservaRodoviario/detalhe/10", HttpMethod.GET).andRespond(withSuccess(DETALHE, MediaType.APPLICATION_JSON));
        var result = service.consultarReserva(consulta(), "USUARIO_TEST"); assertTrue(result.isSuccess()); assertTrue(result.isStatusAtualizado()); server.verify();
    }
    @Test void rejeitaCodigoDeOutraCompraAntesDeAcessarFornecedor() throws Exception {
        expectManager("reservaRodoviario/detalhe/10", HttpMethod.GET).andRespond(withSuccess(DETALHE, MediaType.APPLICATION_JSON));
        var req = consulta(); ((com.fasterxml.jackson.databind.node.ObjectNode) req.getConsulta()).put("codigoCompra", 999);
        var result = service.consultarReserva(req, "USUARIO_TEST"); assertFalse(result.isSuccess()); assertTrue(result.isRequerConferencia()); server.verify();
    }
    @Test void emiteBookingExistenteSemCriarOutroBooking() throws Exception {
        var req = request(); req.setReservaRodoviario(null);
        ((com.fasterxml.jackson.databind.node.ObjectNode) req.getEmissao()).put("codigoReserva", "booking-existente");
        expectHub("consultar").andRespond(withStatus(HttpStatus.NOT_FOUND));
        expectHub("emitir").andExpect(jsonPath("$.codigoReserva").value("booking-existente"))
                .andRespond(withSuccess(envelope(COMPRA), MediaType.APPLICATION_JSON));
        expectHub("consultar").andExpect(jsonPath("$.codigoCompra").value(123456))
                .andRespond(withSuccess(envelope(COMPRA), MediaType.APPLICATION_JSON));
        expectManager("reservaRodoviario/findByLocalizador/123456?codgSistema=9", HttpMethod.GET)
                .andRespond(withSuccess(DETALHE, MediaType.APPLICATION_JSON));
        var result = service.emitir(req, "USUARIO_TEST"); assertTrue(result.isSuccess()); server.verify();
    }
    @Test void registraRecebimentoConciliadoAposSalvarSemNovaCobrancaNoHub() throws Exception {
        var req = request();
        var recebimento = new com.confApi.db.confManager.rodoviario.dto.RegistrarRecebimentoRodoviarioRequest();
        recebimento.setChaveIdempotencia("recebimento-rodoviario-01"); recebimento.setCodgFormaPagto(1);
        recebimento.setValor(new java.math.BigDecimal("22.40")); recebimento.setDataRecebimento(java.time.LocalDate.of(2026, 1, 1));
        recebimento.setCodgTransacao("gateway-0001"); req.setRecebimento(recebimento);
        novoCheckout(COMPRA); semReserva();
        expectManager("reservaRodoviario/salvarCompleta", HttpMethod.POST).andRespond(withSuccess(DETALHE, MediaType.APPLICATION_JSON));
        expectManager("reservaRodoviario/detalhe/10", HttpMethod.GET).andRespond(withSuccess(DETALHE, MediaType.APPLICATION_JSON));
        expectManager("reservaRodoviario/10/recebimentos", HttpMethod.POST)
                .andExpect(jsonPath("$.codgTransacao").value("gateway-0001"))
                .andExpect(jsonPath("$.chaveIdempotencia").value("recebimento-rodoviario-01"))
                .andRespond(withSuccess("{\"codgRecebimento\":500}", MediaType.APPLICATION_JSON));
        expectManager("reservaRodoviario/detalhe/10", HttpMethod.GET).andRespond(withSuccess(DETALHE, MediaType.APPLICATION_JSON));
        var result = service.reservar(req, "USUARIO_TEST"); assertTrue(result.isSuccess()); server.verify();
    }
    @Test void timeoutNaConsultaPreviaImpedeBookingCheckoutEGravacaoDaReserva() throws Exception {
        expectHub("consultar").andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"sistema\":\"QueroPassagem\",\"codigo\":\"FALHA_COMUNICACAO\",\"mensagens\":[\"Falha de comunicacao\"],\"requerConferencia\":false}"));
        var erro = assertThrows(RodoviarioIntegracaoException.class, () -> service.reservar(request(), "USUARIO_TEST"));
        assertEquals(504, erro.getStatus());
        assertEquals("FALHA_COMUNICACAO", erro.getResposta().path("codigo").asText());
        assertFalse(erro.getResposta().path("requerConferencia").asBoolean());
        // Qualquer chamada extra (reservar, emitir ou salvarCompleta) falharia no MockRestServiceServer.
        server.verify();
    }
}
