package com.confApi.chatconfianca.client;

import com.confApi.config.UrlConfig;
import com.confApi.exception.RegraDeNegocioException;
import com.confApi.exception.ServiceIndisponivelException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatConfiancaManagerClientTest {
    private String previousManagerUrl;
    private RestTemplate restTemplate;
    private ChatConfiancaTokenProvider tokenProvider;
    private SimpleMeterRegistry meterRegistry;
    private ChatConfiancaManagerClient client;

    @BeforeEach
    void setUp() {
        previousManagerUrl = UrlConfig.URL_CONFIANCA_MANAGER;
        UrlConfig.URL_CONFIANCA_MANAGER = "http://localhost/manager/";
        restTemplate = mock(RestTemplate.class);
        tokenProvider = mock(ChatConfiancaTokenProvider.class);
        meterRegistry = new SimpleMeterRegistry();
        client = new ChatConfiancaManagerClient(
                restTemplate,
                tokenProvider,
                meterRegistry
        );
    }

    @AfterEach
    void tearDown() {
        UrlConfig.URL_CONFIANCA_MANAGER = previousManagerUrl;
    }

    @Test
    void deveRenovarTokenUmaVezApos401() {
        when(tokenProvider.bearerToken()).thenReturn("token-antigo", "token-novo");
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)))
                .thenThrow(httpError(HttpStatus.UNAUTHORIZED))
                .thenReturn(ResponseEntity.ok("ok"));

        assertEquals("ok", client.get("chat-confianca/consultas/conversas/1", String.class));

        verify(tokenProvider).invalidateIfCurrent("token-antigo");
        verify(tokenProvider, times(2)).bearerToken();
        verify(restTemplate, times(2)).exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class));

        ArgumentCaptor<HttpEntity> entities = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(2)).exchange(
                anyString(),
                eq(HttpMethod.GET),
                entities.capture(),
                eq(String.class));
        assertEquals("Bearer token-antigo",
                entities.getAllValues().get(0).getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        assertEquals("Bearer token-novo",
                entities.getAllValues().get(1).getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        assertEquals(1L, timerCount("GET", "401"));
        assertEquals(1L, timerCount("GET", "2xx"));
    }

    @Test
    void naoDeveRenovarTokenApos403() {
        when(tokenProvider.bearerToken()).thenReturn("token-valido");
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)))
                .thenThrow(httpError(HttpStatus.FORBIDDEN, "detalhe-interno"));

        RegraDeNegocioException error = assertThrows(
                RegraDeNegocioException.class,
                () -> client.get("chat-confianca/consultas/conversas/1", String.class)
        );

        assertEquals(403, error.getStatus());
        assertFalse(error.getMessage().contains("detalhe-interno"));
        assertEquals(1L, timerCount("GET", "403"));
        verify(tokenProvider, never()).invalidateIfCurrent(anyString());
        verify(restTemplate, times(1)).exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class));
    }

    @Test
    void deveEncerrarAposSegundo401SemCriarLoop() {
        when(tokenProvider.bearerToken()).thenReturn("token-antigo", "token-novo");
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenThrow(httpError(HttpStatus.UNAUTHORIZED))
                .thenThrow(httpError(HttpStatus.UNAUTHORIZED));

        assertThrows(
                ServiceIndisponivelException.class,
                () -> client.post("chat-confianca/persistencia/mensagens", "body", String.class)
        );

        verify(tokenProvider).invalidateIfCurrent("token-antigo");
        verify(tokenProvider).invalidateIfCurrent("token-novo");
        verify(restTemplate, times(2)).exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class));
    }

    private HttpClientErrorException httpError(HttpStatus status) {
        return httpError(status, "");
    }

    private HttpClientErrorException httpError(HttpStatus status, String body) {
        return HttpClientErrorException.create(
                status,
                status.getReasonPhrase(),
                HttpHeaders.EMPTY,
                body.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );
    }

    private long timerCount(String method, String outcome) {
        return meterRegistry.get("chatconfianca.manager.requests")
                .tag("method", method)
                .tag("outcome", outcome)
                .timer()
                .count();
    }
}
