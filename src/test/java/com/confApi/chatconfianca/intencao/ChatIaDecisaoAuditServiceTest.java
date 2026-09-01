package com.confApi.chatconfianca.intencao;

import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatIaDecisaoAuditServiceTest {
    private final String urlAnterior = UrlConfig.URL_CONFIANCA_MANAGER;

    @AfterEach
    void restaurarUrl() {
        UrlConfig.URL_CONFIANCA_MANAGER = urlAnterior;
    }

    @Test
    void enviaSomenteMetadadosDaDecisaoAoEndpointRuntime() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ConfAppService confAppService = mock(ConfAppService.class);
        ChatIntencaoShadowProperties properties = new ChatIntencaoShadowProperties();
        properties.setDecisionAuditEnabled(true);
        properties.setVersaoDecisor("decisor-v1.1");
        ConfAppResp token = new ConfAppResp();
        token.setToken("token-de-teste");
        when(confAppService.token()).thenReturn(token);
        UrlConfig.URL_CONFIANCA_MANAGER = "http://manager/";
        ChatIaDecisaoAuditService service = new ChatIaDecisaoAuditService(
                restTemplate, confAppService, properties, Runnable::run);

        ChatConfiancaDecisaoIa decisao = decisao();
        service.registrar(
                10L, 20L, 3, "CGR", decisao,
                true, false, 92L, 321L);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<HttpEntity> entityCaptor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("http://manager/chatMemoria/runtime/decisoes"),
                eq(HttpMethod.POST), entityCaptor.capture(), eq(Void.class));
        ChatIaDecisaoAuditRequest request =
                (ChatIaDecisaoAuditRequest) entityCaptor.getValue().getBody();
        assertEquals("institucional.contatos", request.getIntencaoEfetivaCodigo());
        assertEquals(List.of(7), request.getMemorias());
        assertEquals(91L, request.getDepartamentoSugeridoId());
        assertEquals("SUCESSO", request.getStatusResultado());
        assertEquals(321L, request.getDuracaoTotalMs());
        assertTrue(request.getSugerirAtendente());
        assertFalse(request.getAtendenteSolicitado());
        assertEquals(92L, request.getDepartamentoAtendimentoId());
    }

    @Test
    void flagDesligadaNaoAgendaNemSolicitaToken() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ConfAppService confAppService = mock(ConfAppService.class);
        ChatIntencaoShadowProperties properties = new ChatIntencaoShadowProperties();
        properties.setDecisionAuditEnabled(false);
        ChatIaDecisaoAuditService service = new ChatIaDecisaoAuditService(
                restTemplate, confAppService, properties, Runnable::run);

        service.registrar(10L, 20L, null, "Confianca", decisao(),
                false, false, null, 1L);

        verify(confAppService, never()).token();
        verify(restTemplate, never()).exchange(
                any(String.class), any(HttpMethod.class), any(), eq(Void.class));
    }

    @Test
    void confirmaEncaminhamentoNoUltimoEventoDaConversa() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ConfAppService confAppService = mock(ConfAppService.class);
        ChatIntencaoShadowProperties properties = new ChatIntencaoShadowProperties();
        properties.setDecisionAuditEnabled(true);
        ConfAppResp token = new ConfAppResp();
        token.setToken("token-de-teste");
        when(confAppService.token()).thenReturn(token);
        UrlConfig.URL_CONFIANCA_MANAGER = "http://manager";
        ChatIaDecisaoAuditService service = new ChatIaDecisaoAuditService(
                restTemplate, confAppService, properties, Runnable::run);

        service.registrarEncaminhamento(10L, 92L);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<HttpEntity> entityCaptor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("http://manager/chatMemoria/runtime/decisoes/encaminhamento"),
                eq(HttpMethod.PUT), entityCaptor.capture(), eq(Void.class));
        ChatIaEncaminhamentoAuditRequest request =
                (ChatIaEncaminhamentoAuditRequest) entityCaptor.getValue().getBody();
        assertEquals(10L, request.getConversaId());
        assertEquals(92L, request.getDepartamentoAtendimentoId());
    }

    private ChatConfiancaDecisaoIa decisao() {
        ChatIntencaoClassificacao classificacao =
                ChatIntencaoClassificacao.status("CLASSIFICADA");
        classificacao.setIntencaoId(1L);
        classificacao.setCodigo("institucional.contatos");
        classificacao.setConfianca(92);
        ChatIntencaoRuntimeDto.Memoria memoria = new ChatIntencaoRuntimeDto.Memoria();
        memoria.setCodgMemoria(7);
        DepartamentoUnidade departamento = new DepartamentoUnidade();
        departamento.setId(91L);

        ChatConfiancaDecisaoIa decisao = new ChatConfiancaDecisaoIa();
        decisao.setClassificacaoCatalogo(classificacao);
        decisao.setUnificadaHabilitada(true);
        decisao.setCanarioHabilitado(true);
        decisao.setCanarioElegivel(true);
        decisao.setAplicada(true);
        decisao.setModo("UNIFICADA");
        decisao.setStatus("CLASSIFICADA");
        decisao.setFonte("CHAT_INTENCAO_TERMO_V1");
        decisao.setIntencao("institucional.contatos");
        decisao.setIntencaoLegada("orientacao_geral");
        decisao.setDepartamento(departamento);
        decisao.setDepartamentoConfianca(90);
        decisao.setMemorias(List.of(memoria));
        decisao.setStatusResultado("SUCESSO");
        return decisao;
    }
}
