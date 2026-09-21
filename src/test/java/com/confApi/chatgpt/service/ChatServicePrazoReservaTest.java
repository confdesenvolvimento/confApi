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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatServicePrazoReservaTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private ChatService chat;
    private Reserva reserva;
    private final ConversationRequestDTO request = new ConversationRequestDTO("confia", "Confianca", "321", 321L, 101L,
            "Abra a reserva INM1RT", new ArrayList<>(), null, false, new ArrayList<>());

    @BeforeEach void setup() {
        AereoClient aereo = mock(AereoClient.class);
        AereoRegrasReservaService regras = mock(AereoRegrasReservaService.class);
        chat = spy(new ChatService(null, null, null, null, null, null, null, null, null, null, aereo, regras));
        reserva = new Reserva();
        reserva.setLocalizador("INM1RT");
        reserva.setStatus("Ativa");
        reserva.setPermiteEmitir(true);
        reserva.setDataCriacao(Date.from(Instant.parse("2026-09-14T03:00:00Z")));
        reserva.setPrazoEmissao("2026-09-15T12:43:36.000+00:00");
        ConsultarLocalizadorResponse response = new ConsultarLocalizadorResponse();
        response.setReservas(List.of(reserva));
        when(aereo.carregarReserva(any())).thenReturn(response);
        when(regras.enriquecer(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private JsonNode payload(ChatMessageDTO message) throws Exception {
        String text = message.content();
        return mapper.readTree(text.substring(text.indexOf('{'), text.lastIndexOf('}') + 1)).path("reservas").get(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTC", "America/Sao_Paulo", "America/Cuiaba", "Asia/Tokyo"})
    void criacaoEPrazoChegamProntosAoChatSemDependerDoFusoDoServidor(String zona) throws Exception {
        TimeZone anterior = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zona));
            List<ChatMessageDTO> messages = new ArrayList<>();
            chat.actionApis(messages, request, "reserva_aerea_detalhes", true);
            JsonNode data = payload(messages.get(0));
            assertEquals("14/09/2026 00:00", data.path("dataCriacao").asText());
            assertEquals("15/09/2026 09:43", data.path("prazoEmissao").asText());
            assertTrue(messages.get(0).content().contains("O prazo de emissao"));
        } finally {
            TimeZone.setDefault(anterior);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "2026-09-15T12:43:36Z,15/09/2026 09:43",
        "2026-09-15T09:43:36-03:00,15/09/2026 09:43",
        "2026-09-15T12:43:36.000+0000,15/09/2026 09:43",
        "2026-09-15T18:13:36+05:30,15/09/2026 09:43",
        "2026-09-15T09:43:36,15/09/2026 09:43",
        "2026-09-15T02:59:00Z,14/09/2026 23:59",
        "2026-09-15T03:00:00Z,15/09/2026 00:00",
        "2027-01-01T02:00:00Z,31/12/2026 23:00",
        "2026-09-15,15/09/2026"
    })
    void formataOInstanteDoHubUmaUnicaVezEPreservaDataSemHora(String prazo, String esperado) throws Exception {
        reserva.setPrazoEmissao(prazo);
        JsonNode data = payload(chat.carregarDadosReservaAerea(request, "INM1RT"));
        assertEquals(esperado, data.path("prazoEmissao").asText());
    }

    @Test void aceitaPrazoSerializadoComoMilissegundosPeloHub() throws Exception {
        reserva.setPrazoEmissao(Long.toString(Instant.parse("2026-09-15T12:43:36Z").toEpochMilli()));
        assertEquals("15/09/2026 09:43", payload(chat.carregarDadosReservaAerea(request, "INM1RT"))
                .path("prazoEmissao").asText());
    }

    @Test void regrasEAlertasNaoReintroduzemPrazoBruto() throws Exception {
        ChatMessageDTO message = chat.carregarReservaAereaComRegras(request, "INM1RT");
        JsonNode data = payload(message);
        assertEquals("15/09/2026 09:43", data.path("prazoEmissao").asText());
        assertFalse(message.content().contains(reserva.getPrazoEmissao()));
        List<String> codigos = new ArrayList<>();
        for (JsonNode alerta : data.path("alertasOperacionais")) {
            if (Set.of("PRAZO_EMISSAO", "PENDENTE_EMISSAO").contains(alerta.path("codigo").asText())) {
                codigos.add(alerta.path("codigo").asText());
                assertEquals("15/09/2026 09:43", alerta.path("detalhe").asText());
            }
        }
        assertEquals(Set.of("PRAZO_EMISSAO", "PENDENTE_EMISSAO"), new HashSet<>(codigos));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "invalido", "2026-02-30T12:43:36Z", "2026-09-15T25:43:00Z"})
    void prazoAusenteOuInvalidoNaoInventaDataHoraOuAlerta(String prazo) throws Exception {
        reserva.setPrazoEmissao(prazo);
        JsonNode data = payload(chat.carregarDadosReservaAerea(request, "INM1RT"));
        assertFalse(data.has("prazoEmissao"));
        for (JsonNode alerta : data.path("alertasOperacionais")) {
            assertNotEquals("PRAZO_EMISSAO", alerta.path("codigo").asText());
            if ("PENDENTE_EMISSAO".equals(alerta.path("codigo").asText())) {
                assertFalse(alerta.has("detalhe"));
            }
        }
    }

    @Test void v2EntregaOMesmoPrazoFormatadoAoModelo() throws Exception {
        doReturn(new ChatResponseDTO(null, "Reserva consultada", List.of(), null, List.of(), List.of()))
                .when(chat).chat(any(), any(), any());
        ChatV2Plan plan = ChatV2Plan.of(ChatV2Capability.RESERVA);
        plan.getParametros().put("localizador", "INM1RT");
        new ChatV2Executor(chat, mock(ToolRouter.class), mapper).executar(plan, request, new ChatConfiancaDecisaoIa());
        ArgumentCaptor<ChatRequestDTO> captor = ArgumentCaptor.forClass(ChatRequestDTO.class);
        verify(chat).chat(captor.capture(), any(), any());
        ChatMessageDTO data = captor.getValue().messages().stream()
                .filter(message -> message.content().startsWith("Dado do sistema (reserva_aerea_detalhes):"))
                .findFirst().orElseThrow();
        assertEquals("14/09/2026 00:00", payload(data).path("dataCriacao").asText());
        assertEquals("15/09/2026 09:43", payload(data).path("prazoEmissao").asText());
    }
}
