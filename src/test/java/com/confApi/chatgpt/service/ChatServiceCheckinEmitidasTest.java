package com.confApi.chatgpt.service;

import com.confApi.chatconfianca.intencao.ChatConfiancaDecisaoIa;
import com.confApi.chatconfianca.v2.ChatV2Capability;
import com.confApi.chatconfianca.v2.ChatV2Executor;
import com.confApi.chatconfianca.v2.ChatV2Plan;
import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.tools.ToolRouter;
import com.confApi.db.wooba.checkin.CheckinService;
import com.confApi.db.wooba.checkin.dto.Checkin72Horas;
import com.confApi.db.wooba.checkin.dto.CheckinRQ;
import com.confApi.db.wooba.checkin.dto.TrechoCheckin;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatServiceCheckinEmitidasTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private CheckinService checkin;
    private ChatService chat;

    @BeforeEach void setup() {
        checkin = mock(CheckinService.class);
        chat = spy(new ChatService(null, null, null, null, null, null, checkin, null, null, null, null, null));
    }

    private ConversationRequestDTO request(List<String> keywords) {
        return new ConversationRequestDTO("confia", "Confianca", "321", 321L, 101L,
                "Quais sao meus embarques proximos?", new ArrayList<>(), null, false, keywords);
    }

    private Checkin72Horas reserva(String locator, Integer status) {
        Checkin72Horas value = new Checkin72Horas();
        value.setLocalizadorCompanhia(locator);
        value.setStatusReserva(status);
        value.setNumeroDoBilhete("BILHETE-TESTE-" + locator);
        value.setPassageiro("PASSAGEIRO-TESTE-" + locator);
        value.setLinkCheckin("https://example.invalid/checkin/" + locator);
        TrechoCheckin trecho = new TrechoCheckin();
        trecho.setStatus(1); // Bilhete/trecho ativo nao torna uma reserva cancelada elegivel.
        value.setTrechosMultiplaConexao(List.of(trecho));
        return value;
    }

    private JsonNode reservas(ChatMessageDTO message) throws Exception {
        String text = message.content();
        return mapper.readTree(text.substring(text.indexOf('{'), text.lastIndexOf('}') + 1)).path("reservaCheckInIA");
    }

    @Test void listaMistaRetornaApenasEmitidasSemDadosDeCanceladasOuReservadas() throws Exception {
        when(checkin.findCheckin72Horas(any())).thenReturn(Arrays.asList(
                reserva("GCPKVE", 3), reserva("ATIVA1", 1), reserva("INM1RT", 2),
                reserva("SEMSTS", null), null, reserva("OUTRA2", 2)));
        List<ChatMessageDTO> messages = new ArrayList<>();
        chat.actionApis(messages, request(new ArrayList<>()), "checkin", true);
        JsonNode result = reservas(messages.get(0));
        assertEquals(2, result.size());
        assertEquals("INM1RT", result.get(0).path("localizadorCompanhia").asText());
        assertEquals("OUTRA2", result.get(1).path("localizadorCompanhia").asText());
        for (JsonNode item : result) assertEquals(2, item.path("statusReserva").asInt());
        assertFalse(messages.get(0).content().contains("GCPKVE"));
        assertFalse(messages.get(0).content().contains("ATIVA1"));
        assertFalse(messages.get(0).content().contains("SEMSTS"));
        ArgumentCaptor<CheckinRQ> captor = ArgumentCaptor.forClass(CheckinRQ.class);
        verify(checkin).findCheckin72Horas(captor.capture());
        assertEquals("321", captor.getValue().getIdErp());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, 3, 4, 5, 6, 7, 8, 9, 99})
    void qualquerStatusDiferenteDeEmitidaFicaForaMesmoComBilheteAtivo(int status) throws Exception {
        when(checkin.findCheckin72Horas(any())).thenReturn(List.of(reserva("GCPKVE", status)));
        ChatMessageDTO message = chat.buscarCheckinsProximos(request(new ArrayList<>()));
        assertTrue(reservas(message).isEmpty());
        assertFalse(message.content().contains("GCPKVE"));
    }

    @Test void statusAusenteNaoETratadoComoEmitido() throws Exception {
        when(checkin.findCheckin72Horas(any())).thenReturn(Arrays.asList(reserva("SEMSTS", null), null));
        assertTrue(reservas(chat.buscarCheckinsProximos(request(new ArrayList<>()))).isEmpty());
    }

    @Test void retornoNuloDoServicoContinuaSendoListaVazia() throws Exception {
        when(checkin.findCheckin72Horas(any())).thenReturn(null);
        assertTrue(reservas(chat.buscarCheckinsProximos(request(new ArrayList<>()))).isEmpty());
    }

    @Test void novaConsultaRemoveReservaCanceladaDepoisDaPrimeiraConsulta() throws Exception {
        when(checkin.findCheckin72Horas(any())).thenReturn(
                List.of(reserva("INM1RT", 2)), List.of(reserva("INM1RT", 3)));
        List<ChatMessageDTO> first = new ArrayList<>();
        chat.actionApis(first, request(new ArrayList<>()), "checkin", true);
        assertEquals(1, reservas(first.get(0)).size());
        List<ChatMessageDTO> second = new ArrayList<>();
        chat.actionApis(second, request(new ArrayList<>(List.of("checkin"))), "checkin", true);
        assertEquals(1, second.size());
        assertTrue(reservas(second.get(0)).isEmpty());
        verify(checkin, times(2)).findCheckin72Horas(any());
    }

    @Test void v2NaoGeraCheckinQuandoSoExistemReservasCanceladas() throws Exception {
        when(checkin.findCheckin72Horas(any())).thenReturn(List.of(reserva("GCPKVE", 3)));
        ChatV2Plan plan = ChatV2Plan.of(ChatV2Capability.CHECKIN);
        ChatResponseDTO response = new ChatV2Executor(chat, mock(ToolRouter.class), mapper)
                .executar(plan, request(new ArrayList<>()), new ChatConfiancaDecisaoIa());
        assertEquals("SEM_RESULTADO", plan.getResultado());
        assertTrue(response.content().contains("Não há reservas"));
        assertFalse(response.content().contains("GCPKVE"));
        verify(chat, never()).chat(any(), any(), any());
    }

    @Test void v2EnviaAoModeloSomenteReservasEmitidas() throws Exception {
        when(checkin.findCheckin72Horas(any())).thenReturn(List.of(reserva("GCPKVE", 3), reserva("INM1RT", 2)));
        doReturn(new ChatResponseDTO(null, "Embarque emitido", List.of(), null, List.of(), List.of()))
                .when(chat).chat(any(), any(), any());
        ChatV2Plan plan = ChatV2Plan.of(ChatV2Capability.CHECKIN);
        new ChatV2Executor(chat, mock(ToolRouter.class), mapper)
                .executar(plan, request(new ArrayList<>()), new ChatConfiancaDecisaoIa());
        ArgumentCaptor<ChatRequestDTO> captor = ArgumentCaptor.forClass(ChatRequestDTO.class);
        verify(chat).chat(captor.capture(), any(), any());
        String modelInput = mapper.writeValueAsString(captor.getValue().messages());
        assertTrue(modelInput.contains("INM1RT"));
        assertFalse(modelInput.contains("GCPKVE"));
        assertEquals("DADOS_CONSULTADOS", plan.getResultado());
    }
}
