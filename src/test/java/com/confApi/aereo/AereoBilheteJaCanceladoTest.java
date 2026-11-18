package com.confApi.aereo;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.endPoints.recebimento.RecebimentoApi;
import com.confApi.hub.aereo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AereoBilheteJaCanceladoTest {
    private static final String MOTIVO = "NÃO FOI POSSIVEL CANCELAR O E-TICKET, ACIONE SEU SUPORTE. ERRO:REJECTED - DOCUMENT ALREADY CANCELLED";
    private final String hubOriginal = UrlConfig.URL_CONFIANCA_HUB;
    private final String managerOriginal = UrlConfig.URL_CONFIANCA_MANAGER;
    private final ObjectMapper json = new ObjectMapper();
    private MockRestServiceServer server;
    private AereoClientV2 client;
    private ReservaAereoModel pedido;
    private RecebimentoApi recebimentos;

    @BeforeEach void setup() {
        UrlConfig.URL_CONFIANCA_HUB = "http://fixture/hub/";
        UrlConfig.URL_CONFIANCA_MANAGER = "http://fixture/manager/";
        RestTemplate http = new RestTemplate();
        server = MockRestServiceServer.bindTo(http).build();
        client = new AereoClientV2(http);
        ConfAppService auth = mock(ConfAppService.class);
        ConfAppResp token = new ConfAppResp(); token.setToken("fixture");
        when(auth.token()).thenReturn(token);
        ReflectionTestUtils.setField(client, "confAppService", auth);
        recebimentos = mock(RecebimentoApi.class);
        ReflectionTestUtils.setField(client, "recebimentoApi", recebimentos);
        pedido = new ReservaAereoModel();
        pedido.setCodgReservaAereoDB(77L); pedido.setLocalizador("ABC123"); pedido.setSistema("Wooba");
        var agencia = new com.confApi.db.confManager.agencia.dto.Agencia(99);
        agencia.setCodgUnidade(new com.confApi.db.confManager.unidade.dto.Unidade(7));
        pedido.setAgencia(agencia);
        var pax = new PassageiroModel(); pax.setCodgPassageiroDb(88);
        pax.setBilhetes(new ArrayList<>(List.of(bilhete("999-0000000001"))));
        pedido.setPassageiros(List.of(pax)); pedido.setRecebimentos(List.of());
    }

    @AfterEach void restore() {
        UrlConfig.URL_CONFIANCA_HUB = hubOriginal;
        UrlConfig.URL_CONFIANCA_MANAGER = managerOriginal;
        server.verify();
    }

    @Test void naoAlteraContratoLegadoSemConfirmacoesPorBilhete() {
        var resposta = new com.confApi.aereo.dto.ConsultarEticketResponse();
        assertFalse(json.valueToTree(resposta).has("bilhetesCanceladosFornecedor"));
        resposta.setBilhetesCanceladosFornecedor(List.of());
        assertFalse(json.valueToTree(resposta).has("bilhetesCanceladosFornecedor"));
    }

    @ParameterizedTest @ValueSource(strings = {"Message", "message"})
    void reconciliaRespostaDoIncidenteSemRepetirCancelamento(String campo) throws Exception {
        responder("999-0000000001", Map.of("status", "Não Cancelado", "exception", Map.of(campo, MOTIVO)));
        atualizar(); reler(manager());
        var resposta = client.cancelarBilhete(pedido);
        assertNull(resposta.getException()); assertEquals("Cancelado", resposta.getStatus());
        assertEquals(List.of("9990000000001"), resposta.getBilhetesCanceladosFornecedor());
    }

    @ParameterizedTest @ValueSource(strings = {
        "DOCUMENT ALREADY CANCELLED", "REJECTED - DOCUMENT ALREADY CANCELLED.",
        "  rejected - document\r\n already cancelled  "
    })
    void aceitaVariacoesDoMesmoRetornoExplicito(String motivo) throws Exception {
        responder("999-0000000001", Map.of("exception", Map.of("Message", motivo)));
        atualizar(); reler(manager());
        assertNull(client.cancelarBilhete(pedido).getException());
    }

    @ParameterizedTest @ValueSource(strings = {
        "DOCUMENT NOT CANCELLED", "NOT FOUND", "Não foi possível cancelar o bilhete.",
        "NOT DOCUMENT ALREADY CANCELLED", "DOCUMENT ALREADY CANCELLED?",
        "REJECTED - DOCUMENT ALREADY CANCELLED BUT TICKET IS ACTIVE"
    })
    void naoTransformaOutrasRecusasEmBaixa(String motivo) throws Exception {
        responder("999-0000000001", Map.of("status", "Não Cancelado", "exception", Map.of("Message", motivo)));
        assertNotNull(client.cancelarBilhete(pedido).getException());
        verifyNoInteractions(recebimentos);
    }

    @ParameterizedTest @ValueSource(strings = {"sessaoExpirada", "outroBilhete", "bilheteAtivo", "outraReserva"})
    void naoReconciliaRespostaContraditoria(String caso) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("exception", Map.of("Message", MOTIVO)));
        if (caso.equals("sessaoExpirada")) body.put("sessaoExpirada", true);
        if (caso.equals("outroBilhete")) body.put("bilhete", Map.of("numero", "9990000000002", "status", "Cancelado"));
        if (caso.equals("bilheteAtivo")) body.put("bilhete", Map.of("numero", "9990000000001", "status", "Ativa"));
        if (caso.equals("outraReserva")) body.put("reserva", Map.of("localizador", "OUTRA1"));
        responder("999-0000000001", body);
        assertNotNull(client.cancelarBilhete(pedido).getException());
        verifyNoInteractions(recebimentos);
    }

    @ParameterizedTest @ValueSource(strings = {"ativo", "ausente", "duplicado", "outraReserva", "outroId", "semStatus"})
    void soConfirmaDepoisDaReleituraDoMesmoBilheteNoManager(String caso) throws Exception {
        responder("999-0000000001", Map.of("exception", Map.of("Message", MOTIVO)));
        atualizar();
        Map<String, Object> db = manager();
        Map<String, Object> ticket = new HashMap<>(Map.of("numrBilhete", "9990000000001", "status", 0));
        if (caso.equals("ativo")) ticket.put("status", 1);
        if (caso.equals("semStatus")) ticket.remove("status");
        db.put("passageiros", List.of(Map.of("bilhetes", caso.equals("ausente") ? List.of()
                : caso.equals("duplicado") ? List.of(ticket, ticket) : List.of(ticket))));
        if (caso.equals("outraReserva")) db.put("localizador", "OUTRA1");
        if (caso.equals("outroId")) db.put("codgReservaAereo", 78);
        reler(db);
        if (caso.equals("ativo")) { atualizar(); reler(db); }
        var resposta = client.cancelarBilhete(pedido);
        assertNotNull(resposta.getException());
        assertTrue(resposta.getException().getMessage().contains("Manager não confirmou"));
        verifyNoInteractions(recebimentos);
    }

    @Test void falhaAoGravarNaoConfirmaNemCancelaRecebimentos() throws Exception {
        responder("999-0000000001", Map.of("exception", Map.of("Message", MOTIVO)));
        server.expect(requestTo("http://fixture/manager/bilheteAereo/cancelar/999-0000000001"))
                .andExpect(method(HttpMethod.PUT)).andRespond(withServerError());
        Map<String, Object> ativo = manager();
        ativo.put("passageiros", List.of(Map.of("bilhetes", List.of(Map.of("numrBilhete", "9990000000001", "status", 1)))));
        reler(ativo);
        server.expect(requestTo("http://fixture/manager/bilheteAereo/cancelar/999-0000000001"))
                .andExpect(method(HttpMethod.PUT)).andRespond(withServerError());
        reler(ativo);
        var resposta = client.cancelarBilhete(pedido);
        assertNotNull(resposta.getException());
        assertEquals(List.of("9990000000001"), resposta.getBilhetesCanceladosFornecedor());
        verifyNoInteractions(recebimentos);
    }

    @Test void recuperaFalhaTransitoriaNoManagerSemRecancelarFornecedor() throws Exception {
        responder("999-0000000001", Map.of("status", "Cancelado"));
        server.expect(requestTo("http://fixture/manager/bilheteAereo/cancelar/999-0000000001"))
                .andExpect(method(HttpMethod.PUT)).andRespond(withServerError());
        Map<String, Object> ativo = manager();
        ativo.put("passageiros", List.of(Map.of("bilhetes", List.of(Map.of("numrBilhete", "9990000000001", "status", 1)))));
        reler(ativo); atualizar(); reler(manager());
        assertNull(client.cancelarBilhete(pedido).getException());
    }

    @Test void putComRespostaPerdidaMasGravadoNaoRepeteEscrita() throws Exception {
        responder("999-0000000001", Map.of("status", "Cancelado"));
        server.expect(requestTo("http://fixture/manager/bilheteAereo/cancelar/999-0000000001"))
                .andExpect(method(HttpMethod.PUT)).andRespond(withException(new java.io.IOException("timeout")));
        reler(manager());
        assertNull(client.cancelarBilhete(pedido).getException());
    }

    @Test void bilheteJaCanceladoNaoEncobreRecusaDeOutroBilhete() throws Exception {
        pedido.getPassageiros().get(0).getBilhetes().add(bilhete("9990000000002"));
        var pagamento = new RecebimentoModel(); pagamento.setStatusRecebimento(1); pagamento.setCodgRecebimento(123);
        pedido.setRecebimentos(List.of(pagamento));
        responder("999-0000000001", Map.of("exception", Map.of("Message", MOTIVO)));
        atualizar(); reler(manager());
        responder("9990000000002", Map.of("exception", Map.of("Message", "Cancelamento não autorizado.")));
        var resposta = client.cancelarBilhete(pedido);
        assertNotNull(resposta.getException());
        assertTrue(resposta.getException().getMessage().contains("não autorizado"));
        verifyNoInteractions(recebimentos);
    }

    private static BilheteModel bilhete(String numero) {
        var bilhete = new BilheteModel(); bilhete.setNumeroBilhete(numero); bilhete.setStatus(1); return bilhete;
    }

    private Map<String, Object> manager() {
        return new HashMap<>(Map.of("codgReservaAereo", 77, "localizador", "ABC123", "passageiros",
                List.of(Map.of("bilhetes", List.of(Map.of("numrBilhete", "9990000000001", "status", 0))))));
    }

    private void responder(String numero, Object body) throws Exception {
        server.expect(requestTo("http://fixture/hub/api/aereo/cancelarbilhete"))
                .andExpect(method(HttpMethod.POST)).andExpect(jsonPath("$.eticket").value(numero))
                .andRespond(withSuccess(json.writeValueAsString(body), MediaType.APPLICATION_JSON));
    }

    private void atualizar() {
        server.expect(requestTo("http://fixture/manager/bilheteAereo/cancelar/999-0000000001"))
                .andExpect(method(HttpMethod.PUT)).andExpect(jsonPath("$.status").value(0))
                .andRespond(withSuccess());
    }

    private void reler(Object body) throws Exception {
        server.expect(requestTo("http://fixture/manager/reservaAereo/localizador/ABC123"))
                .andExpect(method(HttpMethod.GET)).andRespond(withSuccess(json.writeValueAsString(body), MediaType.APPLICATION_JSON));
    }
}
