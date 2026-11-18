package com.confApi.db.confManager.regraAereaAlteracao;

import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.chatconfianca.client.ChatConfiancaTokenProvider;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoConsultaRequest;
import com.confApi.exception.ServiceIndisponivelException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RegraAereaAlteracaoManagerServiceTest {
    private static final String REGRA_G3 = """
        {"status":"PERMITIDA_COM_MULTA","mensagem":"Remarcacao permitida",
         "regra":{"id":44,"companhiaAerea":{"iataCia":"G3"},
           "familiaCompanhia":{"nomeFamiliaCompanhia":"LIGHT","codSigla":"LIG","tipoRota":1},
           "permiteAlteracao":true,"exigeRemarcacaoConjunta":true,"statusRevisao":"APROVADO",
           "criterioMultiplosTrechos":"COBRAR_POR_TRECHO","dataCriacao":"2026-07-01T10:00:56"},
         "calculo":{"calculoCompleto":false},"match":{"quantidadeTrechos":2}}
        """;
    private String previousUrl;
    private MockRestServiceServer server;
    private ChatConfiancaTokenProvider tokens;
    private RegraAereaAlteracaoManagerService service;
    private SimpleMeterRegistry metrics;

    @BeforeEach
    void setUp() {
        previousUrl = UrlConfig.URL_CONFIANCA_MANAGER;
        UrlConfig.URL_CONFIANCA_MANAGER = "http://manager.test/confiancamanger4/";
        var rest = new RestTemplate();
        server = MockRestServiceServer.bindTo(rest).build();
        tokens = mock(ChatConfiancaTokenProvider.class);
        when(tokens.bearerToken()).thenReturn("token-valido");
        metrics = new SimpleMeterRegistry();
        service = new RegraAereaAlteracaoManagerService(new ChatConfiancaManagerClient(rest, tokens, metrics));
    }

    @AfterEach
    void tearDown() {
        UrlConfig.URL_CONFIANCA_MANAGER = previousUrl;
        metrics.close();
    }

    @Test
    void consultaRegraG3ComContratoDoManagerEPreservaObrigatoriedadeConjunta() {
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER + "regrasAereas/alteracao/simular"))
            .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization", "Bearer token-valido"))
            .andExpect(jsonPath("$.exigirRegraAprovada").value(true))
            .andExpect(jsonPath("$.companhia").value("G3"))
            .andExpect(jsonPath("$.quantidadeTrechos").value(2))
            .andRespond(withSuccess(REGRA_G3, MediaType.APPLICATION_JSON));
        var response = service.simular(request());
        assertEquals("PERMITIDA_COM_MULTA", response.getStatus());
        assertEquals(44L, response.getRegra().getId());
        assertTrue(response.getRegra().getExigeRemarcacaoConjunta());
        assertFalse(response.getCalculo().getCalculoCompleto());
        server.verify();
    }

    @Test
    void renovaTokenRejeitadoUmaVezSemInterromperSelecaoDoTrecho() {
        when(tokens.bearerToken()).thenReturn("token-antigo", "token-novo");
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER + "regrasAereas/alteracao/simular"))
            .andExpect(header("Authorization", "Bearer token-antigo"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER + "regrasAereas/alteracao/simular"))
            .andExpect(header("Authorization", "Bearer token-novo"))
            .andExpect(jsonPath("$.exigirRegraAprovada").value(true))
            .andRespond(withSuccess(REGRA_G3, MediaType.APPLICATION_JSON));
        assertEquals("PERMITIDA_COM_MULTA", service.simular(request()).getStatus());
        verify(tokens).invalidateIfCurrent("token-antigo");
        verify(tokens, times(2)).bearerToken();
        server.verify();
    }

    @Test
    void interrompeDepoisDaSegundaRecusaDeAutenticacao() {
        when(tokens.bearerToken()).thenReturn("token-antigo", "token-novo");
        for (int i = 0; i < 2; i++) {
            server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER + "regrasAereas/alteracao/simular"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        }
        assertEquals("ERRO_CONSULTA", service.simular(request()).getStatus());
        verify(tokens).invalidateIfCurrent("token-antigo");
        verify(tokens).invalidateIfCurrent("token-novo");
        verify(tokens, times(2)).bearerToken();
        server.verify();
    }

    @Test
    void naoEnviaConsultaAnonimaQuandoAutenticacaoFalha() {
        when(tokens.bearerToken()).thenThrow(new ServiceIndisponivelException("Autenticacao indisponivel"));
        assertEquals("ERRO_CONSULTA", service.simular(request()).getStatus());
        server.verify();
    }

    @Test
    void preservaRecusaComercialSemRepetirConsulta() {
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER + "regrasAereas/alteracao/simular"))
            .andRespond(withSuccess("{\"status\":\"REGRA_NAO_HOMOLOGADA\",\"mensagem\":\"Regra pendente de revisao\"}", MediaType.APPLICATION_JSON));
        var response = service.simular(request());
        assertEquals("REGRA_NAO_HOMOLOGADA", response.getStatus());
        assertEquals("Regra pendente de revisao", response.getMensagem());
        verify(tokens, times(1)).bearerToken();
        server.verify();
    }

    @Test
    void preservaEndpointDeConsultaSemForcarHomologacao() {
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER + "regrasAereas/alteracao/consultar"))
            .andExpect(jsonPath("$.exigirRegraAprovada").value(false))
            .andRespond(withSuccess(REGRA_G3, MediaType.APPLICATION_JSON));
        var request = request();
        request.setExigirRegraAprovada(false);
        assertEquals("PERMITIDA_COM_MULTA", service.consultar(request).getStatus());
        server.verify();
    }

    @Test
    void naoRepeteErroDoServidorNemOConverteEmRegraAprovada() {
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER + "regrasAereas/alteracao/simular"))
            .andRespond(withServerError());
        var response = service.simular(request());
        assertEquals("ERRO_CONSULTA", response.getStatus());
        assertNull(response.getRegra());
        verify(tokens, times(1)).bearerToken();
        server.verify();
    }

    @Test
    void respostaVaziaOuInvalidaPermaneceFalhaTecnica() {
        for (String body : new String[]{"", "null", "{}", "not-json"}) {
            server.reset();
            server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER + "regrasAereas/alteracao/simular"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
            assertEquals("ERRO_CONSULTA", service.simular(request()).getStatus());
            server.verify();
        }
    }

    private RegraAereaAlteracaoConsultaRequest request() {
        var request = new RegraAereaAlteracaoConsultaRequest();
        request.setCompanhia("G3"); request.setFamiliaTarifaria("LIGHT"); request.setCodigoTarifario("LIG");
        request.setMercado("NACIONAL"); request.setClasseReserva("O");
        request.setTipoEvento("REMARCACAO"); request.setMomento("ANTES_EMBARQUE");
        request.setQuantidadeTrechos(2); request.setQuantidadePassageiros(1);
        return request;
    }
}
