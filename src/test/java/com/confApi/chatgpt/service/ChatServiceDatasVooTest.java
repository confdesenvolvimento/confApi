package com.confApi.chatgpt.service;

import com.confApi.aereo.AereoClient;
import com.confApi.aereo.AereoRegrasReservaService;
import com.confApi.aereo.dto.ConsultarLocalizadorResponse;
import com.confApi.aereo.dto.Reserva;
import com.confApi.chatconfianca.intencao.ChatConfiancaDecisaoIa;
import com.confApi.chatconfianca.v2.ChatV2Capability;
import com.confApi.chatconfianca.v2.ChatV2Executor;
import com.confApi.chatconfianca.v2.ChatV2Plan;
import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.tools.ToolRouter;
import com.confApi.hub.aereo.dto.Aeroporto;
import com.confApi.hub.aereo.dto.TrechoReserva;
import com.confApi.hub.aereo.dto.Voo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatServiceDatasVooTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private ChatService chat;
    private Reserva reserva;
    private Voo voo;
    private final ConversationRequestDTO request = new ConversationRequestDTO("confia", "Confianca", "321", 321L, 101L,
            "me retorne os detalhes da reserva INM1RT", new ArrayList<>(), null, false, new ArrayList<>());

    @BeforeEach void setup() {
        AereoClient aereo = mock(AereoClient.class);
        AereoRegrasReservaService regras = mock(AereoRegrasReservaService.class);
        chat = spy(new ChatService(null, null, null, null, null, null, null, null, null, null, aereo, regras));
        reserva = new Reserva();
        reserva.setLocalizador("INM1RT");
        reserva.setStatus("EMITIDA");
        reserva.setDataCriacao(Date.from(Instant.parse("2026-06-14T13:15:00Z")));
        // Fixture baseada no dia corrigido pelo usuario e nos horarios da imagem.
        voo = new Voo();
        voo.setNumeroVoo("4048");
        voo.setOrigem(new Aeroporto("BSB", "Brasilia"));
        voo.setDestino(new Aeroporto("VCP", "Campinas"));
        voo.setDataPartida(Date.from(Instant.parse("2027-02-25T11:25:00Z")));
        voo.setHoraPartida("08:25");
        voo.setDataChegada(Date.from(Instant.parse("2027-02-25T13:05:00Z")));
        voo.setHoraChegada("10:05");
        voo.setDuracao("01h40m");
        TrechoReserva trecho = new TrechoReserva();
        trecho.setVoos(List.of(voo));
        reserva.setViagens(List.of(trecho));
        ConsultarLocalizadorResponse response = new ConsultarLocalizadorResponse();
        response.setReservas(List.of(reserva));
        when(aereo.carregarReserva(any())).thenReturn(response);
        when(regras.enriquecer(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private JsonNode payload(ChatMessageDTO message) throws Exception {
        String text = message.content();
        return mapper.readTree(text.substring(text.indexOf('{'), text.lastIndexOf('}') + 1));
    }

    private JsonNode vooDoPayload(ChatMessageDTO message) throws Exception {
        return payload(message).path("reservas").get(0).path("viagens").get(0).path("voos").get(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTC", "America/Cuiaba", "Asia/Tokyo"})
    void embarqueEm25DeFevereiroChegaFormatadoAoChatSemAlterarHorarios(String serverZone) throws Exception {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(serverZone));
            List<ChatMessageDTO> messages = new ArrayList<>();
            chat.actionApis(messages, request, "reserva_aerea_detalhes", true);
            JsonNode flight = vooDoPayload(messages.get(0));
            assertEquals("25/02/2027", flight.path("dataPartida").asText());
            assertEquals("25/02/2027", flight.path("dataChegada").asText());
            assertEquals("08:25", flight.path("horaPartida").asText());
            assertEquals("10:05", flight.path("horaChegada").asText());
            assertEquals("4048", flight.path("numeroVoo").asText());
            assertEquals("BSB", flight.path("origem").path("iata").asText());
            assertEquals("VCP", flight.path("destino").path("iata").asText());
            assertTrue(flight.path("dataPartida").isTextual());
            assertTrue(flight.path("dataChegada").isTextual());
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "2027-02-26T02:59:00Z,25/02/2027",
            "2027-02-26T03:00:00Z,26/02/2027",
            "2028-01-01T03:00:00Z,01/01/2028",
            "2028-02-29T15:00:00Z,29/02/2028"
    })
    void chegadaUsaSuaPropriaDataMesmoNaViradaDoDiaMesOuAno(String arrival, String expected) throws Exception {
        voo.setDataChegada(Date.from(Instant.parse(arrival)));
        JsonNode flight = vooDoPayload(chat.carregarDadosReservaAerea(request, "INM1RT"));
        assertEquals("25/02/2027", flight.path("dataPartida").asText());
        assertEquals(expected, flight.path("dataChegada").asText());
        assertEquals("10:05", flight.path("horaChegada").asText());
    }

    @Test void consultaDeRegrasTambemPreservaDatasDeVoo() throws Exception {
        JsonNode flight = vooDoPayload(chat.carregarReservaAereaComRegras(request, "INM1RT"));
        assertEquals("25/02/2027", flight.path("dataPartida").asText());
        assertEquals("25/02/2027", flight.path("dataChegada").asText());
    }

    @Test void datasAusentesNaoSaoPreenchidasComCriacaoOuHorarioDoVoo() throws Exception {
        voo.setDataPartida(null);
        voo.setDataChegada(null);
        JsonNode flight = vooDoPayload(chat.carregarDadosReservaAerea(request, "INM1RT"));
        assertFalse(flight.has("dataPartida"));
        assertFalse(flight.has("dataChegada"));
        assertEquals("08:25", flight.path("horaPartida").asText());
        assertEquals("10:05", flight.path("horaChegada").asText());
    }

    @Test void horarioAusenteNaoEInventadoAPartirDoTimestamp() throws Exception {
        voo.setHoraPartida(null);
        voo.setHoraChegada(null);
        JsonNode flight = vooDoPayload(chat.carregarDadosReservaAerea(request, "INM1RT"));
        assertEquals("25/02/2027", flight.path("dataPartida").asText());
        assertEquals("25/02/2027", flight.path("dataChegada").asText());
        assertFalse(flight.has("horaPartida"));
        assertFalse(flight.has("horaChegada"));
    }

    @Test void v2RecebeDatasFormatadasEInstrucaoParaPreservarHorarios() throws Exception {
        doReturn(new ChatResponseDTO(null, "Reserva consultada", List.of(), null, List.of(), List.of()))
                .when(chat).chat(any(), any(), any());
        ChatV2Plan plan = ChatV2Plan.of(ChatV2Capability.RESERVA);
        plan.getParametros().put("localizador", "INM1RT");
        new ChatV2Executor(chat, mock(ToolRouter.class), mapper)
                .executar(plan, request, new ChatConfiancaDecisaoIa());
        ArgumentCaptor<ChatRequestDTO> captor = ArgumentCaptor.forClass(ChatRequestDTO.class);
        verify(chat).chat(captor.capture(), any(), any());
        ChatMessageDTO data = captor.getValue().messages().stream()
                .filter(message -> message.content().startsWith("Dado do sistema (reserva_aerea_detalhes):"))
                .findFirst().orElseThrow();
        assertEquals("25/02/2027", vooDoPayload(data).path("dataPartida").asText());
        assertEquals("25/02/2027", vooDoPayload(data).path("dataChegada").asText());
        assertTrue(data.content().contains("horaPartida e horaChegada"));
        assertEquals("DADOS_CONSULTADOS", plan.getResultado());
    }
}
