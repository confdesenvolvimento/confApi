package com.confApi.hoteis;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.hoteis.model.reserva.HotelCarregaModelFront;
import com.confApi.hub.telegram.TelegramService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HotelReservaSincronizacaoTest {
    private MockRestServiceServer server;
    private HotelClient client;
    private String oldHub, oldManager;

    @BeforeEach void setup() {
        oldHub = UrlConfig.URL_CONFIANCA_HUB;
        oldManager = UrlConfig.URL_CONFIANCA_MANAGER;
        UrlConfig.URL_CONFIANCA_HUB = "https://hub.invalid/";
        UrlConfig.URL_CONFIANCA_MANAGER = "https://manager.invalid";
        RestTemplate rest = new RestTemplate();
        server = MockRestServiceServer.bindTo(rest).build();
        client = new HotelClient(rest);
        var tokens = mock(ConfAppService.class);
        var token = new ConfAppResp(); token.setToken("token-teste");
        when(tokens.token()).thenReturn(token);
        ReflectionTestUtils.setField(client, "confAppService", tokens);
        client.telegramService = mock(TelegramService.class);
    }

    @AfterEach void cleanup() {
        try {
            server.verify();
            verifyNoInteractions(client.telegramService);
        } finally {
            UrlConfig.URL_CONFIANCA_HUB = oldHub;
            UrlConfig.URL_CONFIANCA_MANAGER = oldManager;
        }
    }

    @ParameterizedTest
    @CsvSource({"Confirmed,1,Reserved", "confirmed,1,Reserved", "Reserved,1,Reserved",
        "Cancelled,2,Cancelled", "Canceled,2,Cancelled", "RequestDenied,3,RequestDenied",
        "Rejected,3,RequestDenied", "Modified,4,Modified"})
    void gravaStatusCorretoAntesDeRetornarReserva(String fornecedor, int codigo, String canonico) {
        consulta(fornecedor, 0, "501");
        server.expect(requestTo("https://manager.invalid/reservaHotel/atualizarReserva/42"))
                .andExpect(method(HttpMethod.PUT)).andExpect(jsonPath("$.reservaStatus").value(codigo))
                .andExpect(jsonPath("$.statusPagamento").doesNotExist())
                .andRespond(withSuccess());
        var result = client.carregarReserva(request());
        assertEquals(canonico, result.getReservasHotelRsList().get(0).getStatus());
        assertEquals("https://example.test/salva.jpg", result.getUrlImagem());
        assertEquals("Descrição salva", result.getDescricao());
    }

    @Test void statusIgualNaoGeraAtualizacaoRedundante() {
        consulta("Confirmed", 1, "501");
        assertNotNull(client.carregarReserva(request()));
    }

    @ParameterizedTest @NullAndEmptySource
    @ValueSource(strings = {"Pending", "Mixed", "Cancellation Failed", "Unconfirmed"})
    void statusDesconhecidoNaoGravaZeroNemModificada(String status) {
        fornecedor(status, "501");
        assertFalha(HttpStatus.BAD_GATEWAY);
    }

    @Test void localizadorDivergenteNaoAtualizaOutraReserva() {
        fornecedor("Confirmed", "outra-reserva");
        assertFalha(HttpStatus.BAD_GATEWAY);
    }

    @Test void falhaDePersistenciaNaoRetornaSucesso() {
        consulta("Confirmed", 4, "501");
        server.expect(requestTo("https://manager.invalid/reservaHotel/atualizarReserva/42"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withServerError().body("fixture-privada-nao-imprimir"));
        var erro = assertFalha(HttpStatus.BAD_GATEWAY);
        assertFalse(erro.getMessage().contains("fixture-privada"));
    }

    @Test void timeoutDoHubChegaComo504SemConsultarOuAtualizarManager() {
        server.expect(requestTo("https://hub.invalid/api/hotel/carregarReserva"))
                .andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT)
                        .body("{\"status\":504,\"message\":\"Tempo excedido\"}")
                        .contentType(MediaType.APPLICATION_JSON));
        var erro = assertFalha(HttpStatus.GATEWAY_TIMEOUT);
        var resposta = new com.confApi.exception.GlobalExceptionHandler().handleResponseStatus(erro);
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, resposta.getStatusCode());
        assertTrue(resposta.getBody().getMensagem().contains("tempo de resposta"));
    }

    @Test void timeoutDaConexaoComHubPreserva504() {
        server.expect(requestTo("https://hub.invalid/api/hotel/carregarReserva"))
                .andRespond(request -> { throw new java.net.SocketTimeoutException("Read timed out"); });
        assertFalha(HttpStatus.GATEWAY_TIMEOUT);
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void timeoutDoManagerPreserva504SemAtualizar(boolean timeoutDeConexao) {
        fornecedor("Confirmed", "501");
        var consulta = server.expect(requestTo("https://manager.invalid/reservaHotel/localizador/501"))
                .andExpect(method(HttpMethod.GET));
        if (timeoutDeConexao) {
            consulta.andRespond(request -> { throw new java.net.SocketTimeoutException("fixture-privada"); });
        } else {
            consulta.andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT).body("fixture-privada"));
        }
        var erro = assertFalha(HttpStatus.GATEWAY_TIMEOUT);
        assertFalse(erro.getMessage().contains("fixture-privada"));
    }

    @Test void timeoutAoSincronizarNaoRetornaReservaComoSeTivessePersistido() {
        consulta("Confirmed", 4, "501");
        server.expect(requestTo("https://manager.invalid/reservaHotel/atualizarReserva/42"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(request -> { throw new java.net.SocketTimeoutException("fixture-privada"); });
        var erro = assertFalha(HttpStatus.GATEWAY_TIMEOUT);
        assertFalse(erro.getMessage().contains("fixture-privada"));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void imagemEDescricaoSalvasComplementamFornecedorInclusiveEmPacote(boolean pacote) {
        fornecedorJson("{\"urlImagem\":\" \",\"descricao\":\"\",\"reservasHotelRsList\":["
                + "{\"identificador\":\"501\",\"status\":\"Confirmed\"}]}");
        vinculo(1, pacote);
        var reserva = client.carregarReserva(request());
        assertEquals("https://example.test/salva.jpg", reserva.getUrlImagem());
        assertEquals("Descrição salva", reserva.getDescricao());
        assertEquals("Reserved", reserva.getReservasHotelRsList().get(0).getStatus());
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void imagemEDescricaoDoFornecedorSaoPreservadasInclusiveEmPacote(boolean pacote) {
        fornecedorJson("{\"urlImagem\":\"https://example.test/fornecedor.jpg\","
                + "\"descricao\":\"Descrição do fornecedor\",\"reservasHotelRsList\":["
                + "{\"identificador\":\"501\",\"status\":\"Confirmed\"}]}");
        vinculo(1, pacote);
        var reserva = client.carregarReserva(request());
        assertEquals("https://example.test/fornecedor.jpg", reserva.getUrlImagem());
        assertEquals("Descrição do fornecedor", reserva.getDescricao());
    }

    @Test void avulsoComVariosItensEquivalentesSincronizaUmaUnicaVez() {
        fornecedorJson("{\"reservasHotelRsList\":[{\"identificador\":\"501\",\"status\":\"confirmed\"},"
                + "{\"identificador\":\"501\",\"status\":\"Reserved\"}]}");
        vinculo(0, false);
        server.expect(requestTo("https://manager.invalid/reservaHotel/atualizarReserva/42"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.reservaStatus").value(1))
                .andExpect(jsonPath("$.statusPagamento").doesNotExist())
                .andRespond(withSuccess());
        var itens = client.carregarReserva(request()).getReservasHotelRsList();
        assertEquals(2, itens.size());
        assertTrue(itens.stream().allMatch(item -> "Reserved".equals(item.getStatus())));
    }

    @Test void cancelamentoPreservaCodigoAlfanumericoAteHub() {
        var req = new com.confApi.hoteis.model.reserva.CancelarReservaRequestHotelFront();
        req.setSistema("EZLink"); req.setLocalizador("2385519");
        req.setCodgHotel("684c69793164ee2743353f82");
        server.expect(requestTo("https://hub.invalid/api/hotel/cancelaHotel"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.codgHotel").value("684c69793164ee2743353f82"))
                .andExpect(jsonPath("$.localizador").value("2385519"))
                .andRespond(withSuccess("Cancelado com sucesso!", MediaType.TEXT_PLAIN));
        assertEquals("Cancelado com sucesso!", client.cancelarReserva(req));
    }

    private void consulta(String status, int statusDB, String localizador) {
        fornecedor(status, localizador);
        vinculo(statusDB, false);
    }

    private void fornecedor(String status, String localizador) {
        String valor = status == null ? "null" : "\"" + status + "\"";
        fornecedorJson("{\"reservasHotelRsList\":[{\"identificador\":\"" + localizador
                + "\",\"status\":" + valor + "}]}");
    }

    private void fornecedorJson(String body) {
        server.expect(requestTo("https://hub.invalid/api/hotel/carregarReserva"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void vinculo(int statusDB, boolean pacote) {
        String vinculo = pacote ? "{\"codgPacote\":131,\"estadoEmissao\":\"INICIADA\"}" : "null";
        server.expect(requestTo("https://manager.invalid/reservaHotel/localizador/501"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"codgReservaHotel\":42,\"localizador\":\"501\",\"status\":" + statusDB
                        + ",\"statusPagamento\":1,\"codgReservaPacote\":" + vinculo
                        + ",\"codgHotel\":{\"urlImagemHotel\":\"https://example.test/salva.jpg\","
                        + "\"descricao\":\"Descrição salva\"}}",
                        MediaType.APPLICATION_JSON));
    }

    private ResponseStatusException assertFalha(HttpStatus status) {
        var erro = assertThrows(ResponseStatusException.class, () -> client.carregarReserva(request()));
        assertEquals(status, erro.getStatus());
        assertNull(erro.getCause());
        return erro;
    }

    private HotelCarregaModelFront request() {
        var req = new HotelCarregaModelFront(); req.setIdentificador("501"); return req;
    }
}
