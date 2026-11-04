package com.confApi.db.confManager.rodoviario;

import com.confApi.confApp.*;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.rodoviario.dto.*;
import com.confApi.rodoviario.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RodoviarioManagerClientTest {
    String original;
    RodoviarioManagerClient client;
    MockRestServiceServer server;
    ConfAppService auth;
    @BeforeEach void preparar() {
        original = UrlConfig.URL_CONFIANCA_MANAGER;
        UrlConfig.URL_CONFIANCA_MANAGER = "http://manager.test/confiancamanger2/";
        var rest = new RestTemplate(); auth = mock(ConfAppService.class);
        var token = new ConfAppResp(); token.setToken("APP_TEST"); when(auth.token()).thenReturn(token);
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
        client = new RodoviarioManagerClient(new RodoviarioHttpClient(rest, auth, mapper));
        server = MockRestServiceServer.bindTo(rest).build();
    }
    @AfterEach void restaurar() { UrlConfig.URL_CONFIANCA_MANAGER = original; }
    @Test void encaminhaTokenIndividualEChaveIdempotencia() {
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER + "reservaRodoviario/salvarCompleta"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization", "Bearer APP_TEST"))
                .andExpect(header("X-Usuario-Token", "USUARIO_TEST"))
                .andExpect(jsonPath("$.chaveIdempotencia").value("chave-teste-001"))
                .andRespond(withSuccess("{\"reserva\":{\"codgReservaRodoviario\":10}}", MediaType.APPLICATION_JSON));
        var r = new SalvarReservaRodoviarioRequest(); r.setChaveIdempotencia("chave-teste-001");
        assertEquals(10, client.salvarCompleta(r, "USUARIO_TEST").getReserva().getCodgReservaRodoviario()); server.verify();
    }
    @Test void consultaComBearerDaAplicacaoNaoExigeTokenIndividual() {
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER + "reservaRodoviario/detalhe/10"))
                .andExpect(header("Authorization", "Bearer APP_TEST"))
                .andExpect(headerDoesNotExist("X-Usuario-Token"))
                .andRespond(withSuccess("{\"reserva\":{\"codgReservaRodoviario\":10}}", MediaType.APPLICATION_JSON));
        assertEquals(10, client.detalhe(10, null).getReserva().getCodgReservaRodoviario());
        verify(auth).token(); server.verify();
    }
    @Test void encaminhaIdentificadorDaRequisicaoParaCorrelacionarLogDoManager() {
        org.slf4j.MDC.put(RodoviarioRequestLoggingFilter.REQUEST_ID, "request-manager-123");
        try {
            server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER + "reservaRodoviario/salvarCompleta"))
                    .andExpect(header(RodoviarioRequestLoggingFilter.HEADER, "request-manager-123"))
                    .andRespond(withSuccess("{\"reserva\":{\"codgReservaRodoviario\":10}}", MediaType.APPLICATION_JSON));
            client.salvarCompleta(new SalvarReservaRodoviarioRequest(), "USUARIO_TEST");
            server.verify();
        } finally {
            org.slf4j.MDC.remove(RodoviarioRequestLoggingFilter.REQUEST_ID);
        }
    }
    @Test void preservaErroDeEscopoDoManager() {
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER + "reservaRodoviario/detalhe/10"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON).body("{\"message\":\"Fora do escopo\"}"));
        var ex = assertThrows(RodoviarioIntegracaoException.class, () -> client.detalhe(10, "USUARIO_TEST"));
        assertEquals(403, ex.getStatus()); assertEquals("Fora do escopo", ex.getResposta().path("message").asText()); server.verify();
    }
    @Test void identificaTokenRecusadoPeloManagerSemExporRespostaOriginal() {
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER + "reservaRodoviario/detalhe/10"))
                .andExpect(header("X-Usuario-Token", "USUARIO_EXPIRADO"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"SEGREDO_TOKEN\"}"));
        var ex = assertThrows(RodoviarioIntegracaoException.class, () -> client.detalhe(10, "USUARIO_EXPIRADO"));
        assertEquals(401, ex.getStatus());
        assertEquals("MANAGER", ex.getResposta().path("servico").asText());
        assertEquals("AUTENTICACAO_MANAGER_REJEITADA", ex.getResposta().path("codigo").asText());
        assertTrue(ex.getResposta().path("mensagens").get(0).asText().contains("Autenticacao da aplicacao"));
        assertFalse(ex.getResposta().toString().contains("SEGREDO_TOKEN")); server.verify();
    }
    @Test void codificaFiltrosUmaUnicaVez() {
        var query = new LinkedMultiValueMap<String, String>(); query.add("codgSistema", "9"); query.add("busca", "S\u00e3o Paulo");
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER + "rodoviario/destinos?codgSistema=9&busca=S%C3%A3o%20Paulo"))
                .andRespond(withSuccess("{\"content\":[]}", MediaType.APPLICATION_JSON));
        client.trocar("rodoviario/destinos", HttpMethod.GET, null, query, null, com.fasterxml.jackson.databind.JsonNode.class, false);
        server.verify();
    }
}
