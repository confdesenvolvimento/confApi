package com.confApi.db.wooba.checkin;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.wooba.checkin.dto.CheckinRQ;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CheckinServiceConsultaEstritaTest {
    private RestTemplate rest;
    private CheckinService service;
    private String anterior;
    @BeforeEach void setup() {
        anterior = UrlConfig.URL_CONFIANCA_MANAGER; UrlConfig.URL_CONFIANCA_MANAGER = "https://manager.invalid/";
        rest = mock(RestTemplate.class); service = new CheckinService(rest);
        ConfAppService auth = mock(ConfAppService.class);
        ConfAppResp token = new ConfAppResp(); token.setToken("token-ficticio"); when(auth.token()).thenReturn(token);
        ReflectionTestUtils.setField(service, "confAppService", auth);
    }
    @AfterEach void restore() { UrlConfig.URL_CONFIANCA_MANAGER = anterior; }
    private void response(String body) {
        when(rest.exchange(eq("https://manager.invalid/reservaTrecho/checkin"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(body));
    }
    @Test void listaVaziaRealEhSemResultado() {
        response("[]"); assertTrue(service.findCheckin72HorasEstrito(new CheckinRQ("ERP-SESSAO", 2)).isEmpty());
    }
    @Test void respostaAusenteNaoViraListaVaziaNoChat() {
        response(""); assertThrows(IllegalStateException.class, () -> service.findCheckin72HorasEstrito(new CheckinRQ("ERP-SESSAO", 2)));
    }
    @Test void jsonInvalidoNaoViraListaVaziaNoChat() {
        response("{invalid"); assertThrows(IllegalStateException.class, () -> service.findCheckin72HorasEstrito(new CheckinRQ("ERP-SESSAO", 2)));
    }
    @Test void jsonNullNaoViraListaVaziaNoChat() {
        response("null"); assertThrows(IllegalStateException.class, () -> service.findCheckin72HorasEstrito(new CheckinRQ("ERP-SESSAO", 2)));
    }
    @Test void falhaHttpNaoViraListaVaziaNoChat() {
        when(rest.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class))).thenThrow(new RestClientException("falha simulada"));
        assertThrows(IllegalStateException.class, () -> service.findCheckin72HorasEstrito(new CheckinRQ("ERP-SESSAO", 2)));
    }
    @Test void metodoLegadoForaDoChatMantemCompatibilidade() {
        response(""); assertTrue(service.findCheckin72Horas(new CheckinRQ("ERP-SESSAO", 2)).isEmpty());
    }
}
