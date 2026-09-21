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
import com.confApi.hub.aereo.dto.Bilhete;
import com.confApi.hub.aereo.dto.Passageiro;
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

class ChatServiceDatasReservaTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private ChatService chat;
    private Reserva reserva;
    private final ConversationRequestDTO request = new ConversationRequestDTO("confia", "Confianca", "321", 321L, 101L,
            "me retorne os detalhes da reserva INM1RT", new ArrayList<>(), null, false, new ArrayList<>());

    @BeforeEach void setup() {
        AereoClient aereo = mock(AereoClient.class);
        AereoRegrasReservaService regras = mock(AereoRegrasReservaService.class);
        chat = spy(new ChatService(null, null, null, null, null, null, null, null, null, null, aereo, regras));
        reserva = new Reserva();
        reserva.setLocalizador("INM1RT");
        reserva.setStatus("EMITIDA");
        // A data foi informada pelo usuario; os horarios sao valores sinteticos do teste.
        reserva.setDataCriacao(Date.from(Instant.parse("2026-06-14T13:15:00Z")));
        reserva.setDataEmissao(Date.from(Instant.parse("2026-06-14T14:45:00Z")));
        ConsultarLocalizadorResponse response = new ConsultarLocalizadorResponse();
        response.setReservas(List.of(reserva));
        when(aereo.carregarReserva(any())).thenReturn(response);
        when(regras.enriquecer(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private JsonNode payload(ChatMessageDTO message) throws Exception {
        String text = message.content();
        return mapper.readTree(text.substring(text.indexOf('{'), text.lastIndexOf('}') + 1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTC", "America/Cuiaba", "Asia/Tokyo"})
    void datasDeCriacaoEEmissaoSaoTextosNoMesmoFusoDaTela(String serverZone) throws Exception {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(serverZone));
            List<ChatMessageDTO> messages = new ArrayList<>();
            chat.actionApis(messages, request, "reserva_aerea_detalhes", true);
            JsonNode data = payload(messages.get(0)).path("reservas").get(0);
            assertEquals("14/06/2026 10:15", data.path("dataCriacao").asText());
            assertEquals("14/06/2026 11:45", data.path("dataEmissao").asText());
            assertTrue(data.path("dataCriacao").isTextual());
            assertTrue(data.path("dataEmissao").isTextual());
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "2026-06-15T02:59:00Z,14/06/2026 23:59",
            "2026-06-15T03:00:00Z,15/06/2026 00:00",
            "2027-01-01T02:00:00Z,31/12/2026 23:00",
            "2024-02-29T15:30:00Z,29/02/2024 12:30"
    })
    void converteViradaDoDiaEAnoSemTrocarCriacaoPorEmissao(String instant, String expected) throws Exception {
        reserva.setDataEmissao(Date.from(Instant.parse(instant)));
        JsonNode data = payload(chat.carregarDadosReservaAerea(request, "INM1RT")).path("reservas").get(0);
        assertEquals("14/06/2026 10:15", data.path("dataCriacao").asText());
        assertEquals(expected, data.path("dataEmissao").asText());
    }

    @Test void regrasEEmissaoDoBilheteTambemRecebemDatasFormatadas() throws Exception {
        Bilhete bilhete = new Bilhete();
        bilhete.setDataDeEmissao(reserva.getDataEmissao());
        Passageiro passageiro = new Passageiro();
        passageiro.setNome("Passageiro de teste");
        passageiro.setBilhetes(List.of(bilhete));
        reserva.setPassageiros(List.of(passageiro));
        JsonNode data = payload(chat.carregarReservaAereaComRegras(request, "INM1RT")).path("reservas").get(0);
        assertEquals("14/06/2026 10:15", data.path("dataCriacao").asText());
        assertEquals("14/06/2026 11:45", data.path("dataEmissao").asText());
        assertEquals("14/06/2026 11:45", data.path("passageiros").get(0).path("bilhetes").get(0).path("dataEmissao").asText());
    }

    @Test void datasAusentesContinuamAusentesSemUsarHoje() throws Exception {
        reserva.setDataCriacao(null);
        reserva.setDataEmissao(null);
        ChatMessageDTO message = chat.carregarDadosReservaAerea(request, "INM1RT");
        JsonNode data = payload(message).path("reservas").get(0);
        assertFalse(data.has("dataCriacao"));
        assertFalse(data.has("dataEmissao"));
        assertTrue(message.content().contains("nao informado"));
    }

    @Test void v2EntregaAoModeloAsDatasFormatadasPelaConsulta() throws Exception {
        doReturn(new ChatResponseDTO(null, "Reserva consultada", List.of(), null, List.of(), List.of()))
                .when(chat).chat(any(), any(), any());
        ChatV2Plan plan = ChatV2Plan.of(ChatV2Capability.RESERVA);
        plan.getParametros().put("localizador", "INM1RT");
        ChatV2Executor executor = new ChatV2Executor(chat, mock(ToolRouter.class), mapper);
        executor.executar(plan, request, new ChatConfiancaDecisaoIa());
        ArgumentCaptor<ChatRequestDTO> captor = ArgumentCaptor.forClass(ChatRequestDTO.class);
        verify(chat).chat(captor.capture(), any(), any());
        ChatMessageDTO data = captor.getValue().messages().stream()
                .filter(message -> message.content().startsWith("Dado do sistema (reserva_aerea_detalhes):"))
                .findFirst().orElseThrow();
        assertEquals("14/06/2026 10:15", payload(data).path("reservas").get(0).path("dataCriacao").asText());
        assertEquals("14/06/2026 11:45", payload(data).path("reservas").get(0).path("dataEmissao").asText());
        assertEquals("DADOS_CONSULTADOS", plan.getResultado());
    }
}
