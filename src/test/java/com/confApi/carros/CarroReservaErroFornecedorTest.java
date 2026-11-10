package com.confApi.carros;

import com.confApi.carros.dto.*;
import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.carro.CarroReservaService;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.hub.carro.HubCarroClient;
import com.confApi.model.RecebimentoModel;
import com.confApi.recebimento.RecebimentoService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CarroReservaErroFornecedorTest {
    private static final String FECHADA = "A loja não está aberta no horário selecionado para devolução.";
    private String originalHubUrl;
    private MockRestServiceServer server;
    private HubCarroClient hub;
    private CarroReservaService service;
    private RecebimentoService pagamentos;
    private Recebimento recebimento;
    private CarroCompraModel compra;

    @BeforeEach void setup() {
        originalHubUrl = UrlConfig.URL_CONFIANCA_HUB;
        UrlConfig.URL_CONFIANCA_HUB = "http://hub.test/";
        RestTemplate rest = new RestTemplate();
        server = MockRestServiceServer.bindTo(rest).build();
        hub = new HubCarroClient(rest);
        ConfAppService auth = mock(ConfAppService.class);
        ConfAppResp token = new ConfAppResp(); token.setToken("teste");
        when(auth.token()).thenReturn(token);
        ReflectionTestUtils.setField(hub, "confAppService", auth);
        service = new CarroReservaService(mock(RestTemplate.class));
        pagamentos = mock(RecebimentoService.class);
        ReflectionTestUtils.setField(service, "hubCarroClient", hub);
        ReflectionTestUtils.setField(service, "recebimentoService", pagamentos);
        compra = new CarroCompraModel();
        compra.setReservaCarro(new ReservarCarroRequestDTO());
        compra.setRecebimento(new RecebimentoModel());
        recebimento = new Recebimento();
        recebimento.setCodgRecebimento(123); recebimento.setStatus(1);
        recebimento.setValrRecebimento(200d); recebimento.setValrEntrada(200d);
        when(pagamentos.criarRecebimento(compra)).thenReturn(recebimento);
    }

    @AfterEach void cleanup() {
        UrlConfig.URL_CONFIANCA_HUB = originalHubUrl;
    }

    private void reservaRespondida(String json) {
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_HUB + "api/carro/reservar"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"success\":false,\"reservaCarro\":null,\"error\":[{\"error\":\"STORE_CLOSED\",\"error_description\":\"%s\"}],\"returnMessage\":\"Falha na reserva\"}",
        "{\"success\":false,\"mensagem\":\"%s\"}",
        "{\"success\":false,\"returnMessage\":{\"message\":\"%s\"}}",
        "{\"success\":false,\"returnMessage\":[{\"errorDescription\":\"%s\"}]}",
        "{\"success\":false,\"returnMessage\":\"\",\"mensagem\":\"%s\"}",
        "{\"success\":true,\"error\":[{\"error_description\":\"%s\"}],\"reservaCarro\":{\"bookingID\":\"CAR123\"}}"
    })
    void recusaDaReservaChegaAoAppSemTentarEmitir(String resposta) {
        reservaRespondida("[" + String.format(resposta, FECHADA) + "]");
        CarroReservaOperacaoResponseDTO resultado = service.reservar(compra);
        assertFalse(resultado.getSuccess());
        assertEquals(FECHADA, resultado.getMensagem());
        assertEquals(FECHADA, resultado.getReturnMessage());
        assertNull(resultado.getReserva());
        verify(pagamentos).cancelarRecebimento(recebimento);
        // Só a reserva é esperada: emitir uma recusa falha no MockRestServiceServer.
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "{\"success\":true}", "{\"reservaCarro\":{}}", "{\"reservaCarro\":{\"bookingID\":\" \"}}"})
    void reservaSemBookingValidoRetornaFalhaSemExcecaoOuEmissao(String resposta) {
        reservaRespondida("[" + resposta + "]");
        var resultado = hub.reservar(compra.getReservaCarro());
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertFalse(resultado.get(0).getSuccess());
        assertEquals("Não foi possível concluir a reserva de carro no fornecedor.", resultado.get(0).getMensagem());
        server.verify();
    }

    @Test void erroDaEmissaoTambemChegaAoApp() {
        reservaRespondida("[{\"success\":true,\"tokenSession\":\"selecao\",\"reservaCarro\":{\"bookingID\":\"CAR123\"}}]");
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_HUB + "api/carro/emitir"))
                .andExpect(jsonPath("$.localizador").value("CAR123"))
                .andExpect(jsonPath("$.token").value("selecao"))
                .andRespond(withSuccess("[{\"success\":false,\"error\":[{\"error_description\":\"" + FECHADA + "\"}]}]", MediaType.APPLICATION_JSON));
        var resultado = service.reservar(compra);
        assertFalse(resultado.getSuccess());
        assertEquals(FECHADA, resultado.getMensagem());
        verify(pagamentos).cancelarRecebimento(recebimento);
        server.verify();
    }

    @Test void reservaValidaContinuaEmitindo() {
        reservaRespondida("[{\"success\":true,\"tokenSession\":\"selecao\",\"reservaCarro\":{\"bookingID\":\"CAR123\"}}]");
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_HUB + "api/carro/emitir"))
                .andExpect(jsonPath("$.localizador").value("CAR123"))
                .andExpect(jsonPath("$.token").value("selecao"))
                .andRespond(withSuccess("[{\"success\":true,\"localizador\":\"CAR123\"}]", MediaType.APPLICATION_JSON));
        var resultado = hub.reservar(compra.getReservaCarro());
        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).getSuccess());
        assertEquals("CAR123", resultado.get(0).getLocalizador());
        server.verify();
    }

    @Test void emissaoSemRespostaNaoViraSucesso() {
        reservaRespondida("[{\"success\":true,\"reservaCarro\":{\"bookingID\":\"CAR123\"}}]");
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_HUB + "api/carro/emitir"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        var resultado = hub.reservar(compra.getReservaCarro());
        assertEquals(1, resultado.size());
        assertFalse(resultado.get(0).getSuccess());
        server.verify();
    }
}
