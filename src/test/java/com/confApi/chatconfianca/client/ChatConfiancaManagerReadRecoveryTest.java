package com.confApi.chatconfianca.client;

import com.confApi.config.UrlConfig;
import com.confApi.exception.RegraDeNegocioException;
import com.confApi.exception.ServiceIndisponivelException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.*;
import javax.net.ssl.SSLHandshakeException;
import java.io.*;
import java.net.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatConfiancaManagerReadRecoveryTest {
    private final String original=UrlConfig.URL_CONFIANCA_MANAGER;
    private RestTemplate rest;
    private ChatConfiancaTokenProvider tokens;
    private ChatConfiancaManagerClient client;
    private SimpleMeterRegistry metrics;
    private static final ParameterizedTypeReference<List<String>> LIST = new ParameterizedTypeReference<>() {};

    @BeforeEach void setup() {
        UrlConfig.URL_CONFIANCA_MANAGER="http://fixture/manager/";
        rest=mock(RestTemplate.class);tokens=mock(ChatConfiancaTokenProvider.class);
        when(tokens.bearerToken()).thenReturn("fixture-token");
        metrics=new SimpleMeterRegistry();client=new ChatConfiancaManagerClient(rest,tokens,metrics);
    }
    @AfterEach void restore() { UrlConfig.URL_CONFIANCA_MANAGER=original;metrics.close(); }

    @ParameterizedTest @ValueSource(strings={"socket","eof","connect"})
    void recuperaUmaLeituraAposFalhaRapidaDeConexao(String type) {
        IOException cause=switch(type) {
            case "eof" -> new EOFException("conexao encerrada");
            case "connect" -> new ConnectException("conexao recusada");
            default -> new SocketException("conexao interrompida");
        };
        when(rest.exchange(anyString(),eq(HttpMethod.GET),any(HttpEntity.class),eq(String.class)))
                .thenThrow(new ResourceAccessException("indisponivel",cause)).thenReturn(ResponseEntity.ok("ok"));
        assertEquals("ok",client.get("chat-confianca/persistencia/simulacoes-remarcacao/70",String.class));
        verify(rest,times(2)).exchange(anyString(),eq(HttpMethod.GET),any(HttpEntity.class),eq(String.class));
        verify(tokens,times(1)).bearerToken();verify(tokens,never()).invalidateIfCurrent(anyString());
        assertEquals(1,metrics.get("chatconfianca.manager.requests").tag("outcome","io_error").timer().count());
        assertEquals(1,metrics.get("chatconfianca.manager.requests").tag("outcome","2xx").timer().count());
    }

    @Test void recuperaListaSemRepetirGravacao() {
        when(rest.exchange(anyString(),eq(HttpMethod.GET),any(HttpEntity.class),eq(LIST)))
                .thenThrow(disconnected()).thenReturn(ResponseEntity.ok(List.of("ok")));
        assertEquals(List.of("ok"),client.getList("chat-confianca/consultas/equipes",LIST));
        verify(rest,times(2)).exchange(anyString(),eq(HttpMethod.GET),any(HttpEntity.class),eq(LIST));
    }

    @Test void recuperaCaminhoCodificadoSemAlterarConsulta() {
        URI uri=URI.create("http://fixture/manager/consulta?busca=TESTE%20NOME");
        when(rest.exchange(eq(uri),eq(HttpMethod.GET),any(HttpEntity.class),eq(String.class)))
                .thenThrow(disconnected()).thenReturn(ResponseEntity.ok("ok"));
        assertEquals("ok",client.getEncoded("consulta?busca=TESTE%20NOME",String.class));
        verify(rest,times(2)).exchange(eq(uri),eq(HttpMethod.GET),any(HttpEntity.class),eq(String.class));
    }

    @Test void falhaPersistenteEncerraAposUmaRecuperacao() {
        when(rest.exchange(anyString(),eq(HttpMethod.GET),any(HttpEntity.class),eq(String.class)))
                .thenThrow(disconnected());
        var error=assertThrows(ServiceIndisponivelException.class,()->client.get("consulta",String.class));
        assertTrue(error.getMessage().contains("temporariamente indisponivel"));
        verify(rest,times(2)).exchange(anyString(),eq(HttpMethod.GET),any(HttpEntity.class),eq(String.class));
    }

    @ParameterizedTest @ValueSource(strings={"timeout","tls","dns","conversao"})
    void naoRepeteTimeoutFalhaTlsDnsOuRespostaInvalida(String type) {
        RestClientException error=switch(type) {
            case "timeout" -> new ResourceAccessException("timeout",new SocketTimeoutException("tempo esgotado"));
            case "tls" -> new ResourceAccessException("tls",new SSLHandshakeException("certificado invalido"));
            case "dns" -> new ResourceAccessException("dns",new UnknownHostException("host invalido"));
            default -> new RestClientException("resposta invalida");
        };
        when(rest.exchange(anyString(),eq(HttpMethod.GET),any(HttpEntity.class),eq(String.class))).thenThrow(error);
        assertThrows(ServiceIndisponivelException.class,()->client.get("consulta",String.class));
        verify(rest,times(1)).exchange(anyString(),eq(HttpMethod.GET),any(HttpEntity.class),eq(String.class));
    }

    @Test void respeitaInterrupcaoDaRequisicao() {
        when(rest.exchange(anyString(),eq(HttpMethod.GET),any(HttpEntity.class),eq(String.class))).thenThrow(disconnected());
        try {
            Thread.currentThread().interrupt();
            assertThrows(ServiceIndisponivelException.class,()->client.get("consulta",String.class));
            verify(rest,times(1)).exchange(anyString(),eq(HttpMethod.GET),any(HttpEntity.class),eq(String.class));
        } finally { Thread.interrupted(); }
    }

    @ParameterizedTest @ValueSource(strings={"POST","PUT","DELETE"})
    void nuncaRepeteMutacaoAposFalhaDeConexao(String verb) {
        HttpMethod method=HttpMethod.valueOf(verb);
        when(rest.exchange(anyString(),eq(method),any(HttpEntity.class),eq(Void.class))).thenThrow(disconnected());
        assertThrows(ServiceIndisponivelException.class,()->{
            switch(method) {
                case POST -> client.post("persistencia/preferencia", "body", Void.class);
                case PUT -> client.put("persistencia/preferencia", "body");
                default -> client.delete("persistencia/preferencia");
            }
        });
        verify(rest,times(1)).exchange(anyString(),eq(method),any(HttpEntity.class),eq(Void.class));
    }

    @Test void naoRepetePostComRespostaDeLista() {
        when(rest.exchange(anyString(),eq(HttpMethod.POST),any(HttpEntity.class),eq(LIST))).thenThrow(disconnected());
        assertThrows(ServiceIndisponivelException.class,()->client.postList("persistencia/mensagens","body",LIST));
        verify(rest,times(1)).exchange(anyString(),eq(HttpMethod.POST),any(HttpEntity.class),eq(LIST));
    }

    @Test void resposta503NaoEhTratadaComoSocketInterrompido() {
        when(rest.exchange(anyString(),eq(HttpMethod.GET),any(HttpEntity.class),eq(String.class)))
                .thenThrow(HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE,"",HttpHeaders.EMPTY,new byte[0],null));
        assertThrows(ServiceIndisponivelException.class,()->client.get("consulta",String.class));
        verify(rest,times(1)).exchange(anyString(),eq(HttpMethod.GET),any(HttpEntity.class),eq(String.class));
    }

    @Test void renovacaoDoTokenContinuaDepoisDaRecuperacaoDaConexao() {
        when(tokens.bearerToken()).thenReturn("antigo","novo");
        when(rest.exchange(anyString(),eq(HttpMethod.GET),any(HttpEntity.class),eq(String.class)))
                .thenThrow(disconnected())
                .thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED,"",HttpHeaders.EMPTY,new byte[0],null))
                .thenReturn(ResponseEntity.ok("ok"));
        assertEquals("ok",client.get("consulta",String.class));
        verify(tokens).invalidateIfCurrent("antigo");
        verify(tokens,times(2)).bearerToken();
        verify(rest,times(3)).exchange(anyString(),eq(HttpMethod.GET),any(HttpEntity.class),eq(String.class));
    }

    private ResourceAccessException disconnected() {
        return new ResourceAccessException("indisponivel",new SocketException("conexao interrompida"));
    }
}
