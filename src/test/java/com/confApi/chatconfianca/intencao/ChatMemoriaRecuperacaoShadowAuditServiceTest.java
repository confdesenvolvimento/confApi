package com.confApi.chatconfianca.intencao;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.chatMemoria.ChatMemoriaService;
import com.confApi.db.confManager.chatMemoria.dto.ChatMemoria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatMemoriaRecuperacaoShadowAuditServiceTest {

    private final String urlOriginal = UrlConfig.URL_CONFIANCA_MANAGER;

    @AfterEach
    void restaurarUrl() {
        UrlConfig.URL_CONFIANCA_MANAGER = urlOriginal;
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void comparaRecuperacaoAtualComVinculosSemEnviarConteudo() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ConfAppService confAppService = mock(ConfAppService.class);
        ChatMemoriaService chatMemoriaService = mock(ChatMemoriaService.class);
        ChatIntencaoShadowProperties properties = propriedadesAtivas();
        Executor direto = Runnable::run;
        ChatMemoriaRecuperacaoShadowAuditService service =
                new ChatMemoriaRecuperacaoShadowAuditService(
                        restTemplate, confAppService, chatMemoriaService,
                        properties, direto);
        UrlConfig.URL_CONFIANCA_MANAGER = "http://manager/";

        ChatMemoria memoria1 = memoria(1);
        ChatMemoria memoria2 = memoria(2);
        when(chatMemoriaService.findByBase("Confianca"))
                .thenReturn(List.of(memoria1, memoria2, memoria2));
        ConfAppResp token = new ConfAppResp();
        token.setToken("token-teste");
        when(confAppService.token()).thenReturn(token);
        when(restTemplate.exchange(
                eq("http://manager/chatMemoria/runtime/recuperacoes"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Void.class)))
                .thenReturn(ResponseEntity.status(201).build());

        ChatIntencaoClassificacao sombra = ChatIntencaoClassificacao.status("CLASSIFICADA");
        sombra.setIntencaoId(7L);
        sombra.setCodigo("financeiro.boletos");
        sombra.setConfianca(91);
        sombra.setStatusRecuperacaoMemoria("RECUPERADA");
        sombra.setMemoriasRecuperadas(List.of(2, 3));
        sombra.setTempoRecuperacaoMemoriaMs(1L);

        service.registrar(10L, 20L, "Confianca", sombra);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("http://manager/chatMemoria/runtime/recuperacoes"),
                eq(HttpMethod.POST), captor.capture(), eq(Void.class));
        ChatMemoriaRecuperacaoAuditRequest request =
                (ChatMemoriaRecuperacaoAuditRequest) captor.getValue().getBody();
        assertThat(request.getMemoriasAtuais()).containsExactly(1, 2);
        assertThat(request.getMemoriasNovas()).containsExactly(2, 3);
        assertThat(request.getIntencaoClassificadaCodigo())
                .isEqualTo("financeiro.boletos");
        assertThat(request.getVersaoRecuperador()).isEqualTo("vinculos-v1.0");
        assertThat(request.getTempoAtualMs()).isNotNegative();
        assertThat(request).hasNoNullFieldsOrPropertiesExcept("intencaoClassificadaId");
    }

    @Test
    void flagEspecificaDesabilitadaNaoEnfileiraComparacao() {
        ChatIntencaoShadowProperties properties = propriedadesAtivas();
        properties.setRecoveryAuditEnabled(false);
        Executor executor = mock(Executor.class);
        ChatMemoriaRecuperacaoShadowAuditService service =
                new ChatMemoriaRecuperacaoShadowAuditService(
                        mock(RestTemplate.class), mock(ConfAppService.class),
                        mock(ChatMemoriaService.class), properties, executor);

        service.registrar(
                10L, 20L, "Confianca",
                ChatIntencaoClassificacao.status("CLASSIFICADA"));

        verify(executor, never()).execute(any());
    }

    @Test
    void classificacaoInconclusivaNaoContaminaMetricasDeRecuperacao() {
        ChatIntencaoShadowProperties properties = propriedadesAtivas();
        Executor executor = mock(Executor.class);
        ChatMemoriaRecuperacaoShadowAuditService service =
                new ChatMemoriaRecuperacaoShadowAuditService(
                        mock(RestTemplate.class), mock(ConfAppService.class),
                        mock(ChatMemoriaService.class), properties, executor);

        service.registrar(
                10L, 20L, "Confianca",
                ChatIntencaoClassificacao.status("BAIXA_CONFIANCA"));

        verify(executor, never()).execute(any());
    }

    private ChatIntencaoShadowProperties propriedadesAtivas() {
        ChatIntencaoShadowProperties properties = new ChatIntencaoShadowProperties();
        properties.setShadowEnabled(true);
        properties.setMemoryShadowEnabled(true);
        properties.setRecoveryAuditEnabled(true);
        properties.setVersaoRecuperador("vinculos-v1.0");
        return properties;
    }

    private ChatMemoria memoria(int id) {
        ChatMemoria memoria = new ChatMemoria();
        memoria.setCodgMemoria(id);
        return memoria;
    }
}
