package com.confApi.chatconfianca.intencao;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatIntencaoShadowServiceTest {

    private final String urlOriginal = UrlConfig.URL_CONFIANCA_MANAGER;

    @AfterEach
    void restaurarUrl() {
        UrlConfig.URL_CONFIANCA_MANAGER = urlOriginal;
    }

    @Test
    void desabilitadoNaoConsultaBackendNemClassifica() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ConfAppService confAppService = mock(ConfAppService.class);
        ChatIntencaoShadowProperties properties = new ChatIntencaoShadowProperties();
        properties.setShadowEnabled(false);
        ChatIntencaoShadowService service = new ChatIntencaoShadowService(
                restTemplate, confAppService, new ChatIntencaoTermoClassifier(), properties,
                mock(ChatIntencaoShadowAuditService.class));

        service.atualizarCache();
        ChatIntencaoClassificacao resultado = service.classificar("boleto");

        assertThat(resultado.getStatus()).isEqualTo("DESABILITADO");
        verify(confAppService, never()).token();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void habilitadoClassificaSomenteDepoisDeCarregarCache() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ConfAppService confAppService = mock(ConfAppService.class);
        ChatIntencaoShadowProperties properties = new ChatIntencaoShadowProperties();
        properties.setShadowEnabled(true);
        properties.setMemoryShadowEnabled(true);
        ChatIntencaoShadowService service = new ChatIntencaoShadowService(
                restTemplate, confAppService, new ChatIntencaoTermoClassifier(), properties,
                mock(ChatIntencaoShadowAuditService.class));
        UrlConfig.URL_CONFIANCA_MANAGER = "http://manager/";
        ConfAppResp token = new ConfAppResp();
        token.setToken("token-teste");
        when(confAppService.token()).thenReturn(token);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of(perfilBoleto())));

        assertThat(service.classificar("consultar boleto").getStatus())
                .isEqualTo("AGUARDANDO_CARGA");
        service.atualizarCache();
        ChatIntencaoClassificacao resultado = service.classificar("Quero consultar meu boleto");

        assertThat(resultado.getStatus()).isEqualTo("CLASSIFICADA");
        assertThat(resultado.getCodigo()).isEqualTo("financeiro.boletos");
        assertThat(resultado.getStatusRecuperacaoMemoria()).isEqualTo("RECUPERADA");
        assertThat(resultado.getMemoriasRecuperadas()).containsExactly(25);
        assertThat(service.getAtualizadoEm()).isNotNull();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void falhaTransitoriaAoCarregarClassificadorRealizaNovaTentativa() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ConfAppService confAppService = mock(ConfAppService.class);
        ChatIntencaoShadowProperties properties = new ChatIntencaoShadowProperties();
        properties.setShadowEnabled(true);
        ChatIntencaoShadowService service = new ChatIntencaoShadowService(
                restTemplate, confAppService, new ChatIntencaoTermoClassifier(), properties,
                mock(ChatIntencaoShadowAuditService.class));
        UrlConfig.URL_CONFIANCA_MANAGER = "http://manager/";
        ConfAppResp token = new ConfAppResp();
        token.setToken("token-teste");
        when(confAppService.token()).thenReturn(token);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenThrow(new ResourceAccessException("conexao abortada"))
                .thenReturn(ResponseEntity.ok(List.of(perfilBoleto())));

        service.atualizarCache();

        assertThat(service.classificar("Quero consultar meu boleto").getStatus())
                .isEqualTo("CLASSIFICADA");
        assertThat(service.getAtualizadoEm()).isNotNull();
        verify(restTemplate, times(2)).exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void recuperaSomenteMemoriasCompativeisComUnidadeOuBase() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ConfAppService confAppService = mock(ConfAppService.class);
        ChatIntencaoShadowProperties properties = new ChatIntencaoShadowProperties();
        properties.setShadowEnabled(true);
        properties.setMemoryShadowEnabled(true);
        ChatIntencaoShadowService service = new ChatIntencaoShadowService(
                restTemplate, confAppService, new ChatIntencaoTermoClassifier(), properties,
                mock(ChatIntencaoShadowAuditService.class));
        UrlConfig.URL_CONFIANCA_MANAGER = "http://manager/";
        ConfAppResp token = new ConfAppResp();
        token.setToken("token-teste");
        when(confAppService.token()).thenReturn(token);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of(perfilBoletoComEscopos())));

        service.atualizarCache();
        ChatIntencaoClassificacao resultado = service.classificar(
                "Quero consultar meu boleto", 7, "Unidade Cuiabá");

        assertThat(resultado.getStatusRecuperacaoMemoria()).isEqualTo("RECUPERADA");
        assertThat(resultado.getMemoriasRecuperadas()).containsExactly(25, 26, 28);
        assertThat(resultado.getMemoriasDetalhadas())
                .extracting(ChatIntencaoRuntimeDto.Memoria::getCodgMemoria)
                .containsExactly(25, 26, 28);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void semEscopoNaoRecuperaMemoriaEspecifica() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ConfAppService confAppService = mock(ConfAppService.class);
        ChatIntencaoShadowProperties properties = new ChatIntencaoShadowProperties();
        properties.setShadowEnabled(true);
        properties.setMemoryShadowEnabled(true);
        ChatIntencaoShadowService service = new ChatIntencaoShadowService(
                restTemplate, confAppService, new ChatIntencaoTermoClassifier(), properties,
                mock(ChatIntencaoShadowAuditService.class));
        UrlConfig.URL_CONFIANCA_MANAGER = "http://manager/";
        ConfAppResp token = new ConfAppResp();
        token.setToken("token-teste");
        when(confAppService.token()).thenReturn(token);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of(perfilBoletoComEscopos())));

        service.atualizarCache();
        ChatIntencaoClassificacao resultado = service.classificar(
                "Quero consultar meu boleto");

        assertThat(resultado.getMemoriasRecuperadas()).containsExactly(25);
    }

    private ChatIntencaoRuntimeDto perfilBoleto() {
        ChatIntencaoRuntimeDto perfil = new ChatIntencaoRuntimeDto();
        perfil.setId(15L);
        perfil.setCodigo("financeiro.boletos");
        perfil.setNome("Consulta de boletos");
        ChatIntencaoRuntimeDto.Termo termo = new ChatIntencaoRuntimeDto.Termo();
        termo.setTermo("boleto");
        termo.setTermoNormalizado("boleto");
        termo.setPeso(new BigDecimal("10.000"));
        termo.setPolaridade("POSITIVA");
        perfil.setTermos(List.of(termo));
        ChatIntencaoRuntimeDto.Memoria memoria = new ChatIntencaoRuntimeDto.Memoria();
        memoria.setCodgMemoria(25);
        memoria.setBase("geral");
        memoria.setTexto("Dados do boleto");
        memoria.setPrioridade(100);
        perfil.setMemorias(List.of(memoria));
        return perfil;
    }

    private ChatIntencaoRuntimeDto perfilBoletoComEscopos() {
        ChatIntencaoRuntimeDto perfil = perfilBoleto();
        perfil.setMemorias(List.of(
                memoria(25, "geral", null),
                memoria(26, "Unidade Cuiaba", null),
                memoria(27, "Unidade Palmas", null),
                memoria(28, "Unidade Palmas", 7),
                memoria(29, "geral", 8),
                memoria(30, null, null)));
        return perfil;
    }

    private ChatIntencaoRuntimeDto.Memoria memoria(Integer id,
                                                   String base,
                                                   Integer codgUnidade) {
        ChatIntencaoRuntimeDto.Memoria memoria = new ChatIntencaoRuntimeDto.Memoria();
        memoria.setCodgMemoria(id);
        memoria.setBase(base);
        memoria.setCodgUnidade(codgUnidade);
        memoria.setTexto("Memoria " + id);
        memoria.setPrioridade(100);
        return memoria;
    }
}
