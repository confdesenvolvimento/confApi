package com.confApi.hoteis;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.hoteis.model.reserva.HotelCarregaModelFront;
import com.confApi.hub.telegram.TelegramService;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HotelClientConsultaPacoteTest {
    private static final String HUB = "https://hub.test/api/hotel/carregarReserva";
    private static final String MANAGER = "https://manager.test/reservaHotel/localizador/HOT-42";
    private final RestTemplate http = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(http).build();
    private final HotelClient client = new HotelClient(http);
    private final ConfAppService auth = mock(ConfAppService.class);
    private final TelegramService telegram = mock(TelegramService.class);
    private final HotelCarregaModelFront request = new HotelCarregaModelFront("HOT-42");
    private String originalHub;
    private String originalManager;

    @BeforeEach void setup() {
        originalHub = UrlConfig.URL_CONFIANCA_HUB;
        originalManager = UrlConfig.URL_CONFIANCA_MANAGER;
        UrlConfig.URL_CONFIANCA_HUB = "https://hub.test/";
        UrlConfig.URL_CONFIANCA_MANAGER = "https://manager.test";
        ReflectionTestUtils.setField(client, "confAppService", auth);
        client.telegramService = telegram;
        ConfAppResp token = new ConfAppResp(); token.setToken("token-fixture");
        when(auth.token()).thenReturn(token);
    }

    @AfterEach void cleanup() {
        UrlConfig.URL_CONFIANCA_HUB = originalHub;
        UrlConfig.URL_CONFIANCA_MANAGER = originalManager;
        server.verify();
        verifyNoInteractions(telegram);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Confirmed", "Reserved", "Cancelled"})
    void pacoteEmEmissaoConsultaFornecedorSemPutNemEmissao(String status) {
        fornecedor(status);
        vinculo("{\"codgReservaHotel\":42,\"localizador\":\"HOT-42\","
                + "\"codgReservaPacote\":{\"codgPacote\":131,\"estadoEmissao\":\"INICIADA\"}}");
        var reserva = client.carregarReserva(request);
        assertNotNull(reserva);
        assertEquals("HOT-42", reserva.getReservasHotelRsList().get(0).getIdentificador());
        assertEquals(status, reserva.getReservasHotelRsList().get(0).getStatus());
        verify(auth).token();
        // O MockRestServiceServer aceita somente as duas consultas; qualquer PUT/POST adicional falha.
    }

    @ParameterizedTest
    @CsvSource({"Confirmed,1", "Cancelled,2", "Rejected,3", "Reserved,0"})
    void avulsoExplicitamenteSemPacotePreservaSincronizacaoLegada(String status, int esperado) {
        fornecedor(status);
        vinculo("{\"codgReservaHotel\":42,\"localizador\":\"HOT-42\",\"codgReservaPacote\":null}");
        server.expect(requestTo("https://manager.test/reservaHotel/atualizarReserva/42"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.codgReservaHotel").value(42))
                .andExpect(jsonPath("$.reservaStatus").value(esperado))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertNotNull(client.carregarReserva(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "null", "{}", "[]",
        "{\"codgReservaHotel\":42,\"localizador\":\"HOT-42\"}",
        "{\"codgReservaHotel\":42,\"localizador\":\"OUTRO\",\"codgReservaPacote\":null}",
        "{\"codgReservaHotel\":0,\"localizador\":\"HOT-42\",\"codgReservaPacote\":null}",
        "{\"codgReservaHotel\":\"42\",\"localizador\":\"HOT-42\",\"codgReservaPacote\":null}",
        "{\"codgReservaHotel\":42,\"localizador\":\"HOT-42\",\"codgReservaPacote\":{}}",
        "{\"codgReservaHotel\":42,\"localizador\":\"HOT-42\",\"codgReservaPacote\":{\"codgPacote\":0}}",
        "{\"codgReservaHotel\":42,\"localizador\":\"HOT-42\",\"codgReservaPacote\":{\"codgPacote\":\"131\"}}",
        "{\"codgReservaHotel\":42,\"localizador\":\"HOT-42\",\"codgReservaPacote\":131}",
        "{\"codgReservaHotel\":42,\"localizador\":\"HOT-42\",\"codgPacote\":131}",
        "{\"status\":500,\"mensagem\":\"Erro interno\"}"
    })
    void vinculoNuloOmitidoOuInvalidoNaoEInterpretadoComoAvulso(String body) {
        fornecedor("Confirmed");
        vinculo(body);
        assertThrows(IllegalStateException.class, () -> client.carregarReserva(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "{\"reservasHotelRsList\":[]}",
        "{\"reservasHotelRsList\":[{\"identificador\":\"OUTRO\",\"status\":\"Confirmed\"}]}",
        "{\"reservasHotelRsList\":[{\"identificador\":\"HOT-42\"}]}",
        "{\"reservasHotelRsList\":[null]}"})
    void fornecedorSemIdentidadeEEstadoNaoConsultaVinculoNemSincroniza(String body) {
        server.expect(requestTo(HUB)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThrows(IllegalStateException.class, () -> client.carregarReserva(request));
    }

    @Test void erroDoManagerPreservaFalhaSemPutELogaSomenteCategoriaTecnica() {
        fornecedor("Confirmed");
        server.expect(requestTo(MANAGER)).andRespond(withStatus(HttpStatus.CONFLICT)
                .body("{\"mensagem\":\"fixture-privada-nao-imprimir\"}").contentType(MediaType.APPLICATION_JSON));
        try (Logs logs = new Logs()) {
            var erro = assertThrows(IllegalStateException.class, () -> client.carregarReserva(request));
            assertNull(erro.getCause());
            assertFalse(erro.getMessage().contains("fixture-privada"));
            assertTrue(logs.textos.stream().anyMatch(s -> s.contains("fase=CONSULTA_VINCULO") && s.contains("HTTP=409")));
            assertTrue(logs.textos.stream().noneMatch(s -> s.contains("fixture-privada") || s.contains("token-fixture")));
            assertTrue(logs.registros.stream().allMatch(r -> r.getThrown() == null));
        }
    }

    @Test void pedidoSemIdentificadorNaoAutenticaNemConsulta() {
        assertThrows(IllegalStateException.class, () -> client.carregarReserva(new HotelCarregaModelFront(" ")));
        verifyNoInteractions(auth);
    }

    private void fornecedor(String status) {
        server.expect(requestTo(HUB)).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.identificador").value("HOT-42"))
                .andExpect(header("Authorization", "Bearer token-fixture"))
                .andRespond(withSuccess("{\"reservasHotelRsList\":[{\"identificador\":\"HOT-42\",\"status\":\""
                        + status + "\"}]}", MediaType.APPLICATION_JSON));
    }
    private void vinculo(String body) {
        server.expect(requestTo(MANAGER)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private static final class Logs extends Handler implements AutoCloseable {
        private final Logger logger = Logger.getLogger(HotelClient.class.getName());
        private final List<String> textos = new ArrayList<>();
        private final List<LogRecord> registros = new ArrayList<>();
        Logs() { logger.addHandler(this); }
        @Override public void publish(LogRecord r) {
            registros.add(r);
            textos.add(r.getParameters() == null ? r.getMessage() : MessageFormat.format(r.getMessage(), r.getParameters()));
        }
        @Override public void flush() { }
        @Override public void close() { logger.removeHandler(this); }
    }
}
