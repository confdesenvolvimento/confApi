package com.confApi.chatgpt.service;

import com.confApi.chatgpt.dto.*;
import com.confApi.db.wooba.checkin.CheckinService;
import com.confApi.db.wooba.checkin.dto.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatServiceCheckinDeterministicoTest {
    private ConversationRequestDTO sessao(Long agencia, String erp) {
        return new ConversationRequestDTO("confia", "CGB", erp, agencia, 20L, "Quais são meus próximos check-ins?",
                List.of(new ChatMessageDTO("assistant", "Reserva antiga FLSEMF")), null, false, List.of("reserva_aerea_detalhes"));
    }
    @Test void respondeSemModeloSemLocalizadorAntigoEComAgenciaDaSessao() throws Exception {
        CheckinService service = mock(CheckinService.class);
        ChatService chat = spy(new ChatService(null, null, null, null, null, null, service, null, null, null, null, null));
        Checkin72Horas reserva = new Checkin72Horas(); reserva.setStatusReserva(2); reserva.setLocalizadorCompanhia("ABC123");
        TrechoCheckin voo = new TrechoCheckin(); voo.setData(new Date()); voo.setDe("CGB"); voo.setPara("GRU"); voo.setHora("12:00");
        reserva.setTrechosMultiplaConexao(List.of(voo));
        when(service.findCheckin72HorasEstrito(any())).thenReturn(List.of(reserva));
        ChatResponseDTO result = chat.responderCheckinsProximos(sessao(10L, "ERP-SESSAO"));
        assertTrue(result.content().contains("ABC123")); assertFalse(result.content().contains("FLSEMF"));
        assertFalse(result.content().matches("(?s).*\\b[0-9]{13}\\b.*"));
        ArgumentCaptor<CheckinRQ> rq = ArgumentCaptor.forClass(CheckinRQ.class);
        verify(service).findCheckin72HorasEstrito(rq.capture()); assertEquals("ERP-SESSAO", rq.getValue().getIdErp());
        verify(chat, never()).chat(any(), any(), any());
    }
    @Test void falhaDeIntegracaoNaoEhApresentadaComoAusenciaDeReservas() {
        CheckinService service = mock(CheckinService.class);
        when(service.findCheckin72HorasEstrito(any())).thenThrow(new IllegalStateException("detalhe interno"));
        ChatService chat = new ChatService(null, null, null, null, null, null, service, null, null, null, null, null);
        var result = chat.responderCheckinsProximos(sessao(10L, "ERP-SESSAO"));
        assertTrue(result.content().contains("Não foi possível consultar"));
        assertFalse(result.content().contains("Não há reservas")); assertFalse(result.content().contains("detalhe interno"));
        assertTrue(result.history().get(0).content().contains("ERRO_INTEGRACAO"));
    }
    @Test void semContextoAutorizadoNaoConsultaServico() {
        CheckinService service = mock(CheckinService.class);
        ChatService chat = new ChatService(null, null, null, null, null, null, service, null, null, null, null, null);
        var result = chat.responderCheckinsProximos(sessao(null, "ERP-SESSAO"));
        assertTrue(result.history().get(0).content().contains("CONSULTA_BLOQUEADA")); verifyNoInteractions(service);
    }
}
