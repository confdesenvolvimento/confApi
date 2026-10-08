package com.confApi.db.confManager.sistema;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SistemaServiceTest {
    SistemaService service;
    MockRestServiceServer server;
    String urlAnterior;

    @BeforeEach void setup() {
        urlAnterior = UrlConfig.URL_CONFIANCA_MANAGER;
        UrlConfig.URL_CONFIANCA_MANAGER = "http://localhost/manager/";
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        ConfAppService auth = mock(ConfAppService.class);
        ConfAppResp token = new ConfAppResp();
        token.setToken("token-teste");
        when(auth.token()).thenReturn(token);
        service = new SistemaService(restTemplate, auth);
    }

    @AfterEach void close() { UrlConfig.URL_CONFIANCA_MANAGER = urlAnterior; }

    @Test void consultaManagerAutenticadoEFiltraApenasProdutoSolicitadoSemFiltrarStatus() {
        server.expect(requestTo("http://localhost/manager/sistema?produto.codgProduto=2"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-teste"))
                .andRespond(withSuccess("""
                        [
                          {"nomeSistema":"Omnibees","status":1,"produto":{"codgProduto":2}},
                          {"nomeSistema":"EZLink","status":2,"produto":{"codgProduto":2}},
                          {"nomeSistema":"HotelDO","status":0,"produto":{"codgProduto":2}},
                          {"nomeSistema":"Azul","produto":{"codgProduto":1}},
                          {"nomeSistema":"SemProduto"},
                          null
                        ]
                        """, MediaType.APPLICATION_JSON));
        var sistemas = service.findByCodgProduto(2);
        assertEquals(java.util.List.of("Omnibees", "EZLink", "HotelDO"),
                sistemas.stream().map(Sistema::getNomeSistema).toList());
        server.verify();
    }

    @Test void aceitaCadastroVazio() {
        server.expect(requestTo("http://localhost/manager/sistema?produto.codgProduto=2"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertTrue(service.findByCodgProduto(2).isEmpty());
        server.verify();
    }

    @Test void falhaDoManagerNaoViraListaVazia() {
        server.expect(requestTo("http://localhost/manager/sistema?produto.codgProduto=2"))
                .andRespond(withServerError());
        assertEquals(502, assertThrows(ResponseStatusException.class,
                () -> service.findByCodgProduto(2)).getRawStatusCode());
        server.verify();
    }

    @Test void respostaSemCorpoNaoViraListaVazia() {
        server.expect(requestTo("http://localhost/manager/sistema?produto.codgProduto=2"))
                .andRespond(withSuccess());
        assertEquals(502, assertThrows(ResponseStatusException.class,
                () -> service.findByCodgProduto(2)).getRawStatusCode());
        server.verify();
    }
}
