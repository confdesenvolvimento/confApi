package com.confApi.aereo;

import com.confApi.aereo.dto.ConsultarLocalizadorRequest;
import com.confApi.aereo.dto.ConsultarLocalizadorResponse;
import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AereoClientConsultaEstritaTest {
    private final RestTemplate rest = mock(RestTemplate.class);
    private final ConfAppService auth = mock(ConfAppService.class);
    private final AereoClient client = new AereoClient(rest);
    private final ConsultarLocalizadorRequest request = new ConsultarLocalizadorRequest();

    @BeforeEach void setup() {
        ReflectionTestUtils.setField(client, "confAppService", auth);
        ConfAppResp token = new ConfAppResp();
        token.setToken("token-teste");
        when(auth.token()).thenReturn(token);
        request.setLocalizador("DQFDHL");
    }

    @Test void consultaValidaVaziaPreservaRespostaSemFabricarErro() {
        ConsultarLocalizadorResponse response = new ConsultarLocalizadorResponse();
        when(rest.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ConsultarLocalizadorResponse.class)))
                .thenReturn(ResponseEntity.ok(response));
        assertSame(response, client.carregarReservaEstrita(request));
        verify(rest).exchange(endsWith("/api/aereo/consultar"), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(ConsultarLocalizadorResponse.class));
    }

    @Test void timeoutNaConsultaEstritaNaoRetornaListaVazia() {
        when(rest.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ConsultarLocalizadorResponse.class)))
                .thenThrow(new ResourceAccessException("timeout"));
        assertThrows(IllegalStateException.class, () -> client.carregarReservaEstrita(request));
    }

    @Test void respostaSemCorpoNaoConfirmaReservaInexistente() {
        when(rest.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ConsultarLocalizadorResponse.class)))
                .thenReturn(ResponseEntity.noContent().build());
        assertThrows(IllegalStateException.class, () -> client.carregarReservaEstrita(request));
    }

    @Test void erroHttpNaoPodeSerConsideradoConsultaVazia() {
        when(rest.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ConsultarLocalizadorResponse.class)))
                .thenReturn(ResponseEntity.status(503).build());
        assertThrows(IllegalStateException.class, () -> client.carregarReservaEstrita(request));
    }

    @Test void falhaAutenticacaoNaoConfirmaReservaInexistente() {
        when(auth.token()).thenThrow(new IllegalStateException("auth indisponivel"));
        assertThrows(IllegalStateException.class, () -> client.carregarReservaEstrita(request));
        verifyNoInteractions(rest);
    }

    @Test void consumidoresLegadosPreservamRetornoPadrao() {
        when(rest.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ConsultarLocalizadorResponse.class)))
                .thenThrow(new ResourceAccessException("timeout"));
        ConsultarLocalizadorResponse response = client.carregarReserva(request);
        assertNotNull(response);
        assertTrue(response.getReservas().isEmpty());
        assertNull(response.getException());
    }
}
