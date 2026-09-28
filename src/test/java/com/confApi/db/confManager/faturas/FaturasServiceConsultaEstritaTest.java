package com.confApi.db.confManager.faturas;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.faturas.dto.FaturaSicaRQ;
import com.confApi.db.confManager.faturas.dto.FaturaSicaRS;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FaturasServiceConsultaEstritaTest {
    private RestTemplate restTemplate;
    private ConfAppService confAppService;
    private FaturasService service;
    private String urlAnterior;

    @BeforeEach void preparar() {
        urlAnterior = UrlConfig.URL_CONFIANCA_MANAGER;
        UrlConfig.URL_CONFIANCA_MANAGER = "https://manager.invalid/";
        restTemplate = mock(RestTemplate.class);
        confAppService = mock(ConfAppService.class);
        ConfAppResp credencial = new ConfAppResp();
        credencial.setToken("token-sintetico-teste");
        when(confAppService.token()).thenReturn(credencial);
        service = new FaturasService(restTemplate);
        ReflectionTestUtils.setField(service, "confAppService", confAppService);
    }

    @AfterEach void restaurarUrl() {
        UrlConfig.URL_CONFIANCA_MANAGER = urlAnterior;
    }

    private FaturaSicaRQ consulta() {
        FaturaSicaRQ rq = new FaturaSicaRQ();
        rq.setEmpfat("321");
        rq.setPagamento("PAGO");
        rq.setTipoData("VENCIMENTO");
        rq.setDataInicio("2026-08-01");
        rq.setDataFim("2026-08-31");
        return rq;
    }

    private void responder(ResponseEntity<List<FaturaSicaRS>> resposta) {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class),
                org.mockito.ArgumentMatchers.<ParameterizedTypeReference<List<FaturaSicaRS>>>any()))
                .thenReturn(resposta);
    }

    private void falhar(RuntimeException falha) {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class),
                org.mockito.ArgumentMatchers.<ParameterizedTypeReference<List<FaturaSicaRS>>>any()))
                .thenThrow(falha);
    }

    @Test void listaVaziaValidaRepresentaConsultaConcluidaSemFaturas() {
        responder(ResponseEntity.ok(List.of()));
        assertTrue(service.faturaSicaEstrita(consulta()).isEmpty());
    }

    @Test void retornaFaturasSemAlterarFiltrosOuIdentidadeDoRequest() {
        FaturaSicaRQ rq = consulta();
        FaturaSicaRS fatura = new FaturaSicaRS();
        fatura.setNumfat(12345);
        List<FaturaSicaRS> resultado = List.of(fatura);
        responder(ResponseEntity.ok(resultado));
        assertSame(resultado, service.faturaSicaEstrita(rq));
        verify(restTemplate).exchange(argThat((URI uri) -> uri.getPath().endsWith("faturaSica/listarFaturas")),
                eq(HttpMethod.POST), argThat((HttpEntity<?> entity) -> entity.getBody() == rq
                        && "Bearer token-sintetico-teste".equals(entity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))),
                org.mockito.ArgumentMatchers.<ParameterizedTypeReference<List<FaturaSicaRS>>>any());
        assertEquals("321", rq.getEmpfat());
        assertEquals("PAGO", rq.getPagamento());
        assertEquals("2026-08-01", rq.getDataInicio());
        assertEquals("2026-08-31", rq.getDataFim());
    }

    @Test void corpoNuloEmHttp200NaoSignificaSemFaturas() {
        responder(ResponseEntity.ok(null));
        assertThrows(FaturasService.ConsultaFaturasException.class, () -> service.faturaSicaEstrita(consulta()));
    }

    @Test void http204NaoSignificaSemFaturas() {
        responder(ResponseEntity.noContent().build());
        assertThrows(FaturasService.ConsultaFaturasException.class, () -> service.faturaSicaEstrita(consulta()));
    }

    @Test void respostaNulaNaoSignificaSemFaturas() {
        responder(null);
        assertThrows(FaturasService.ConsultaFaturasException.class, () -> service.faturaSicaEstrita(consulta()));
    }

    @Test void statusInvalidoComListaNaoViraConsultaConcluida() {
        responder(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(List.of()));
        assertThrows(FaturasService.ConsultaFaturasException.class, () -> service.faturaSicaEstrita(consulta()));
    }

    @Test void erroHttpPreservaCausaSemExporCorpoNaMensagemPublica() {
        RuntimeException falha = HttpServerErrorException.create(HttpStatus.BAD_GATEWAY, "erro", HttpHeaders.EMPTY,
                "dados-financeiros-sinteticos-sensiveis".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        falhar(falha);
        FaturasService.ConsultaFaturasException erro = assertThrows(FaturasService.ConsultaFaturasException.class,
                () -> service.faturaSicaEstrita(consulta()));
        assertSame(falha, erro.getCause());
        assertFalse(erro.getMessage().contains("sensiveis"));
    }

    @Test void timeoutNaoViraListaVazia() {
        RuntimeException falha = new ResourceAccessException("timeout sintetico");
        falhar(falha);
        assertSame(falha, assertThrows(FaturasService.ConsultaFaturasException.class,
                () -> service.faturaSicaEstrita(consulta())).getCause());
    }

    @Test void erroDeDesserializacaoNaoViraListaVazia() {
        RuntimeException falha = new RestClientException("erro sintetico ao converter resposta");
        falhar(falha);
        assertSame(falha, assertThrows(FaturasService.ConsultaFaturasException.class,
                () -> service.faturaSicaEstrita(consulta())).getCause());
    }

    @Test void falhaDeAutenticacaoNaoViraListaVaziaNemEnviaRequest() {
        when(confAppService.token()).thenThrow(new IllegalStateException("falha sintetica autenticacao"));
        assertThrows(FaturasService.ConsultaFaturasException.class, () -> service.faturaSicaEstrita(consulta()));
        verifyNoInteractions(restTemplate);
    }

    @Test void consultaNulaFalhaSemChamadaHttp() {
        assertThrows(FaturasService.ConsultaFaturasException.class, () -> service.faturaSicaEstrita(null));
        verifyNoInteractions(restTemplate);
    }

    @Test void contratoLegadoContinuaTolerandoCorpoNulo() {
        responder(ResponseEntity.ok(null));
        assertTrue(service.faturaSica(consulta()).isEmpty());
    }
}
