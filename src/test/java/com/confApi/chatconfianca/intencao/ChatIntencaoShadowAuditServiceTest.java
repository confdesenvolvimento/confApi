package com.confApi.chatconfianca.intencao;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatIntencaoShadowAuditServiceTest {

    private final String urlOriginal = UrlConfig.URL_CONFIANCA_MANAGER;

    @AfterEach
    void restaurarUrl() {
        UrlConfig.URL_CONFIANCA_MANAGER = urlOriginal;
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void registraResultadoSemEnviarTextoDaMensagem() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ConfAppService confAppService = mock(ConfAppService.class);
        ChatIntencaoShadowProperties properties = new ChatIntencaoShadowProperties();
        properties.setShadowEnabled(true);
        properties.setAuditEnabled(true);
        properties.setVersaoClassificador("termos-v1.0");
        Executor direto = Runnable::run;
        ChatIntencaoShadowAuditService service = new ChatIntencaoShadowAuditService(
                restTemplate, confAppService, properties, direto);
        UrlConfig.URL_CONFIANCA_MANAGER = "http://manager/";
        ConfAppResp token = new ConfAppResp();
        token.setToken("token-teste");
        when(confAppService.token()).thenReturn(token);
        when(restTemplate.exchange(
                eq("http://manager/chatMemoria/runtime/classificacoes"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Void.class)))
                .thenReturn(ResponseEntity.status(201).build());

        ChatIntencaoClassificacao sombra = ChatIntencaoClassificacao.status("CLASSIFICADA");
        sombra.setIntencaoId(7L);
        sombra.setCodigo("financeiro.boletos");
        sombra.setScore(new BigDecimal("10.000"));
        sombra.setConfianca(85);
        sombra.setTermosPositivos(List.of("boleto"));

        service.registrar(10L, 20L, "financeiro", sombra);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("http://manager/chatMemoria/runtime/classificacoes"),
                eq(HttpMethod.POST),
                captor.capture(),
                eq(Void.class));
        ChatIntencaoShadowAuditRequest request =
                (ChatIntencaoShadowAuditRequest) captor.getValue().getBody();
        assertThat(request.getConversaId()).isEqualTo(10L);
        assertThat(request.getMensagemId()).isEqualTo(20L);
        assertThat(request.getIntencaoClassificadaId()).isEqualTo(7L);
        assertThat(request.getTermosPositivos()).containsExactly("boleto");
    }

    @Test
    void flagDesabilitadaNaoEnfileiraAuditoria() {
        ChatIntencaoShadowProperties properties = new ChatIntencaoShadowProperties();
        properties.setShadowEnabled(true);
        properties.setAuditEnabled(false);
        Executor executor = mock(Executor.class);
        ChatIntencaoShadowAuditService service = new ChatIntencaoShadowAuditService(
                mock(RestTemplate.class), mock(ConfAppService.class), properties, executor);

        service.registrar(
                10L, 20L, "financeiro", ChatIntencaoClassificacao.status("CLASSIFICADA"));

        verify(executor, never()).execute(any());
    }
}
