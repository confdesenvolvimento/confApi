package com.confApi.aereo;

import com.confApi.aereo.dto.PesquisaRequestDTO;
import com.confApi.aereo.dto.PesquisaResponse;
import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
class AereoClientPesquisaEstritaTest {
    private final RestTemplate rest = mock(RestTemplate.class);
    private final ConfAppService auth = mock(ConfAppService.class);
    private final AereoClient client = new AereoClient(rest);
    private final PesquisaRequestDTO request = new PesquisaRequestDTO();

    @BeforeEach void setup() {
        com.confApi.config.UrlConfig.URL_CONFIANCA_HUB = "http://localhost.invalid";
        ReflectionTestUtils.setField(client, "confAppService", auth);
        ConfAppResp token = new ConfAppResp();
        token.setToken("token-teste");
        when(auth.token()).thenReturn(token);
    }

    @Test void respostaVaziaValidaNaoEErro() {
        when(rest.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of()));
        assertTrue(client.pesquisarDisponibilidadeEstrita(request).isEmpty());
    }

    @Test void respostaComOpcoesMantemContrato() {
        List<PesquisaResponse> retorno = List.of(new PesquisaResponse());
        when(rest.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(retorno));
        assertSame(retorno, client.pesquisarDisponibilidadeEstrita(request));
    }

    @Test void timeoutTemCodigoSeguroSemPayloadNemCausa() {
        when(rest.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenThrow(new ResourceAccessException("payload-privado", new SocketTimeoutException("segredo")));
        AereoClient.ConsultaDisponibilidadeException erro = assertThrows(AereoClient.ConsultaDisponibilidadeException.class,
                () -> client.pesquisarDisponibilidadeEstrita(request));
        assertEquals("TIMEOUT", erro.getCodigo());
        assertFalse(erro.getMessage().contains("payload-privado"));
        assertNull(erro.getCause());
    }

    @Test void status503NaoViraListaVazia() {
        when(rest.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.status(503).build());
        assertEquals("RESPOSTA_INVALIDA", assertThrows(AereoClient.ConsultaDisponibilidadeException.class,
                () -> client.pesquisarDisponibilidadeEstrita(request)).getCodigo());
    }

    @Test void respostaSemCorpoNaoViraListaVazia() {
        when(rest.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.noContent().build());
        assertEquals("RESPOSTA_INVALIDA", assertThrows(AereoClient.ConsultaDisponibilidadeException.class,
                () -> client.pesquisarDisponibilidadeEstrita(request)).getCodigo());
    }

    @Test void falhaAutenticacaoNaoInvocaFornecedor() {
        when(auth.token()).thenThrow(new IllegalStateException("token-privado"));
        assertEquals("ERRO_CONSULTA", assertThrows(AereoClient.ConsultaDisponibilidadeException.class,
                () -> client.pesquisarDisponibilidadeEstrita(request)).getCodigo());
        verifyNoInteractions(rest);
    }

    @Test void consumidorLegadoAindaRecebeFallback() {
        when(rest.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenThrow(new ResourceAccessException("falha teste"));
        assertTrue(client.pesquisarDisponibilidade(request).isEmpty());
    }
}
