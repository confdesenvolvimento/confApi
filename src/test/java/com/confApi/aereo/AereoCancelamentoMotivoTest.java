package com.confApi.aereo;

import com.confApi.confApp.*;
import com.confApi.config.UrlConfig;
import com.confApi.hub.aereo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AereoCancelamentoMotivoTest {
    private final String hubOriginal=UrlConfig.URL_CONFIANCA_HUB;
    private final String managerOriginal=UrlConfig.URL_CONFIANCA_MANAGER;
    private final ObjectMapper json=new ObjectMapper();
    private AereoClientV2 client;
    private MockRestServiceServer server;
    private ReservaAereoModel entrada;
    private static final String GENERICO="O fornecedor não confirmou o cancelamento do bilhete.";

    @BeforeEach void setup() {
        UrlConfig.URL_CONFIANCA_HUB="http://fixture/hub/";
        UrlConfig.URL_CONFIANCA_MANAGER="http://fixture/manager/";
        RestTemplate rest=new RestTemplate();server=MockRestServiceServer.bindTo(rest).build();
        client=new AereoClientV2(rest);
        ConfAppService auth=mock(ConfAppService.class);ConfAppResp token=new ConfAppResp();token.setToken("fixture");
        when(auth.token()).thenReturn(token);ReflectionTestUtils.setField(client,"confAppService",auth);
        entrada=new ReservaAereoModel();entrada.setCodgReservaAereoDB(77L);entrada.setLocalizador("ABC123");entrada.setSistema("Wooba");
        var agencia=new com.confApi.db.confManager.agencia.dto.Agencia(99);
        agencia.setCodgUnidade(new com.confApi.db.confManager.unidade.dto.Unidade(7));entrada.setAgencia(agencia);
        var pax=new PassageiroModel();pax.setCodgPassageiroDb(88);
        var bilhete=new BilheteModel();bilhete.setNumeroBilhete("999-0000000001");bilhete.setStatus(1);
        pax.setBilhetes(List.of(bilhete));entrada.setPassageiros(List.of(pax));entrada.setRecebimentos(List.of());
    }
    @AfterEach void restore() { UrlConfig.URL_CONFIANCA_HUB=hubOriginal;UrlConfig.URL_CONFIANCA_MANAGER=managerOriginal; }

    @ParameterizedTest @ValueSource(strings={"Message","message"})
    void motivoDoFornecedorChegaAoAppSemAtualizarManager(String campo) throws Exception {
        responder(Map.of("status","Não Cancelado","exception",Map.of(campo,"Bilhete não localizado no fornecedor.")));
        var resposta=client.cancelarBilhete(entrada);
        assertEquals("Não Cancelado",resposta.getStatus());
        assertEquals(GENERICO+" Motivo: Bilhete não localizado no fornecedor.",resposta.getException().getMessage());
        server.verify();
    }

    @Test void motivoPrevaleceMesmoComStatusContraditorio() throws Exception {
        responder(Map.of("status","Cancelado","exception",Map.of("Message","Cancelamento não autorizado.")));
        var resposta=client.cancelarBilhete(entrada);
        assertEquals("Não Cancelado",resposta.getStatus());
        assertTrue(resposta.getException().getMessage().contains("Cancelamento não autorizado."));server.verify();
    }

    @ParameterizedTest @ValueSource(strings={"", "   "})
    void erroSemDetalheMantemMensagemGenerica(String motivo) throws Exception {
        responder(Map.of("status","Não Cancelado","exception",Map.of("Message",motivo)));
        assertEquals(GENERICO,client.cancelarBilhete(entrada).getException().getMessage());server.verify();
    }

    @Test void statusNaoConfirmadoSemExceptionTambemFalha() throws Exception {
        responder(Map.of("status","Não Cancelado"));
        assertEquals(GENERICO,client.cancelarBilhete(entrada).getException().getMessage());server.verify();
    }

    @Test void normalizaQuebrasDeLinhaDoMotivo() throws Exception {
        responder(Map.of("status","Não Cancelado","exception",Map.of("Message","  Prazo\r\n de cancelamento\tencerrado.  ")));
        assertEquals(GENERICO+" Motivo: Prazo de cancelamento encerrado.",client.cancelarBilhete(entrada).getException().getMessage());server.verify();
    }

    @ParameterizedTest @ValueSource(strings={"Falha senha='segredo-teste'","Falha Password: segredo-teste","Bearer segredo-teste","{\"Senha\":\"segredo-teste\"}","<html>erro interno</html>"})
    void naoExibeCredenciaisOuHtmlNoDetalhe(String motivo) throws Exception {
        responder(Map.of("status","Não Cancelado","exception",Map.of("Message",motivo)));
        assertEquals(GENERICO,client.cancelarBilhete(entrada).getException().getMessage());server.verify();
    }

    @Test void naoExibeDumpMuitoLongo() throws Exception {
        responder(Map.of("status","Não Cancelado","exception",Map.of("Message","x".repeat(1001))));
        assertEquals(GENERICO,client.cancelarBilhete(entrada).getException().getMessage());server.verify();
    }

    @Test void falhaDeTransporteNaoRepeteCancelamento() {
        server.expect(requestTo("http://fixture/hub/api/aereo/cancelarbilhete"))
                .andRespond(withException(new IOException("conexao interrompida")));
        var resposta=client.cancelarBilhete(entrada);
        assertEquals("Não Cancelado",resposta.getStatus());
        assertEquals("O HUB não confirmou o cancelamento do bilhete.",resposta.getException().getMessage());server.verify();
    }

    @Test void sucessoContinuaExigindoAtualizacaoEReleituraDoManager() throws Exception {
        responder(Map.of("status","Cancelado"));
        server.expect(requestTo("http://fixture/manager/bilheteAereo/cancelar/999-0000000001"))
                .andExpect(method(HttpMethod.PUT)).andExpect(jsonPath("$.status").value(0)).andRespond(withSuccess());
        server.expect(requestTo("http://fixture/manager/reservaAereo/localizador/ABC123"))
                .andExpect(method(HttpMethod.GET)).andRespond(withSuccess("{\"passageiros\":[{\"bilhetes\":[{\"numrBilhete\":\"999-0000000001\",\"status\":0}]}]}",MediaType.APPLICATION_JSON));
        var resposta=client.cancelarBilhete(entrada);assertNull(resposta.getException());assertEquals("Cancelado",resposta.getStatus());server.verify();
    }

    private void responder(Object body) throws Exception {
        server.expect(requestTo("http://fixture/hub/api/aereo/cancelarbilhete"))
                .andExpect(method(HttpMethod.POST)).andExpect(jsonPath("$.eticket").value("999-0000000001"))
                .andRespond(withSuccess(json.writeValueAsString(body),MediaType.APPLICATION_JSON));
    }
}
