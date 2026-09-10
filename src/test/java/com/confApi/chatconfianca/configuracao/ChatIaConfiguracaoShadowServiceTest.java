package com.confApi.chatconfianca.configuracao;

import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.intencao.*;
import com.confApi.chatconfianca.v2.*;
import com.confApi.confApp.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ChatIaConfiguracaoShadowServiceTest {
    private String urlOriginal;
    @org.junit.jupiter.api.BeforeEach void configurarUrlFicticia() {
        urlOriginal = com.confApi.config.UrlConfig.URL_CONFIANCA_MANAGER;
        com.confApi.config.UrlConfig.URL_CONFIANCA_MANAGER = "http://manager.invalid/";
    }
    @org.junit.jupiter.api.AfterEach void restaurarUrl() {
        com.confApi.config.UrlConfig.URL_CONFIANCA_MANAGER = urlOriginal;
    }
    ChatConfiancaDecisaoIa decisao() {
        var d = new ChatConfiancaDecisaoIa(); d.setAplicada(true); d.setIntencao("financeiro.limites");
        var cl = ChatIntencaoClassificacao.status("CLASSIFICADA"); cl.setCodigo("financeiro.limites"); cl.setConfianca(95);
        d.setClassificacaoCatalogo(cl); return d;
    }
    ChatIaConfiguracaoShadowService service(RestTemplate http, ConfAppService auth, Executor executor, int percent) {
        return new ChatIaConfiguracaoShadowService(http, auth, executor, new ObjectMapper().findAndRegisterModules(), percent);
    }
    @Test void defaultOffNaoCriaWorkerClienteOuServico() {
        new ApplicationContextRunner().withUserConfiguration(ChatIaConfiguracaoShadowConfig.class,
                ChatIaConfiguracaoShadowService.class).run(ctx -> {
            assertNull(ctx.getStartupFailure()); assertTrue(ctx.getBeansOfType(ChatIaConfiguracaoShadowService.class).isEmpty());
            assertFalse(ctx.containsBean("chatIaConfiguracaoShadowExecutor"));
            assertFalse(ctx.containsBean("chatIaConfiguracaoShadowHttp"));
        });
    }
    @Test void amostraZeroNaoConsultaNemEnfileira() {
        var http = mock(RestTemplate.class); var auth = mock(ConfAppService.class); var executor = mock(Executor.class);
        service(http, auth, executor, 0).observar(1L, 2L, "PMW", 7, decisao(), null, false, List.of());
        verifyNoInteractions(http, auth, executor);
    }
    @Test void filaCheiaNaoImpactaResposta() {
        var http = mock(RestTemplate.class); var auth = mock(ConfAppService.class); var executor = mock(Executor.class);
        doThrow(new RejectedExecutionException()).when(executor).execute(any());
        assertDoesNotThrow(() -> service(http, auth, executor, 100).observar(1L, 2L, "PMW", 7, decisao(), null, false, List.of()));
        verifyNoInteractions(http, auth);
    }
    @Test void trabalhoSoExecutaNoWorkerEFalhaPausaProximasConsultas() {
        var http = mock(RestTemplate.class); var auth = mock(ConfAppService.class); var executor = mock(Executor.class);
        when(auth.token()).thenThrow(new IllegalStateException("Nao deve aparecer no log"));
        var service = service(http, auth, executor, 100);
        service.observar(1L, 2L, "PMW", 7, decisao(), null, false, List.of());
        verifyNoInteractions(http, auth);
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class); verify(executor).execute(task.capture());
        assertDoesNotThrow(task.getValue()::run);
        service.observar(1L, 3L, "PMW", 7, decisao(), null, false, List.of());
        verify(executor, times(1)).execute(any()); verify(auth, times(1)).token(); verifyNoInteractions(http);
    }
    @Test void consultaHttpAutenticadaDeMetadadosSemMensagemOuParametros() {
        RestTemplate http = new RestTemplate(); var server = MockRestServiceServer.bindTo(http).build();
        var auth = mock(ConfAppService.class); var token = mock(ConfAppResp.class);
        when(auth.token()).thenReturn(token); when(token.getToken()).thenReturn("token-de-teste");
        server.expect(requestTo(org.hamcrest.Matchers.endsWith("chatIa/runtime/configuracao?intencao=financeiro.limites")))
                .andExpect(method(HttpMethod.GET)).andExpect(header("Authorization", "Bearer token-de-teste"))
                .andExpect(content().string(""))
                .andRespond(withSuccess("{\"versaoContrato\":\"ia-config-shadow-v1\",\"configuracao\":null,\"etapas\":[],\"conhecimentos\":[]}", MediaType.APPLICATION_JSON));
        service(http, auth, Runnable::run, 100).observar(1L, 2L, "PMW", 7, decisao(), null, false, List.of());
        server.verify();
    }
    @Test void naoReaproveitaScoreDeOutraIntencaoNemInventaScoreSemantico() {
        var d = decisao(); d.getClassificacaoCatalogo().setCodigo("institucional.suporte_ti");
        d.setFonte("V2_SEMANTICA");
        var ctx = ChatIaConfiguracaoShadowService.capturar(1L, 2L, "PMW", 7, d, null, false, List.of());
        assertNull(ctx.confianca()); assertEquals("financeiro.limites", ctx.intencaoConsulta());
        d.setFonte("REGRA_DETERMINISTICA");
        assertEquals(100, ChatIaConfiguracaoShadowService.capturar(1L, 2L, "PMW", 7, d, null, false, List.of()).confianca());
    }
    @Test void snapshotNaoRetemEstadoMutavelETemDepartamentoMestreNaoLotacao() {
        var d = decisao(); var m = new ChatIntencaoRuntimeDto.Memoria(); m.setCodgMemoria(10); m.setTexto("conteudo privado"); d.getMemorias().add(m);
        var dep = new DepartamentoUnidade(); dep.setId(55L); dep.setDepartamentoId(99L); dep.setCodgUnidade(7); dep.setAtivo(true);
        var ctx = ChatIaConfiguracaoShadowService.capturar(1L, 2L, "PMW", 7, d, null, false, List.of(dep));
        d.getMemorias().clear(); dep.setDepartamentoId(100L);
        assertEquals(Set.of(10), ctx.memoriasAtuais()); assertEquals(Set.of(99L), ctx.departamentosPermitidos());
        assertFalse(ctx.toString().contains("conteudo privado"));
        assertThrows(UnsupportedOperationException.class, () -> ctx.memoriasAtuais().clear());
    }
    @Test void handoffUsaAssuntoSemHerdarScoreErradoOuDepartamentoDeOutraUnidade() {
        var d = decisao(); d.setIntencao(ChatV2Capability.HUMANO.code);
        var p = ChatV2Plan.of(ChatV2Capability.HUMANO); p.setAssuntoHandoff("institucional.suporte_ti");
        var dep = new DepartamentoUnidade(); dep.setDepartamentoId(99L); dep.setCodgUnidade(8); dep.setAtivo(true);
        var ctx = ChatIaConfiguracaoShadowService.capturar(1L, 2L, "PMW", 7, d, p, false, List.of(dep));
        assertTrue(ctx.humanoSolicitado()); assertEquals("institucional.suporte_ti", ctx.intencaoConsulta());
        assertNull(ctx.confianca()); assertTrue(ctx.departamentosPermitidos().isEmpty());
    }
}
