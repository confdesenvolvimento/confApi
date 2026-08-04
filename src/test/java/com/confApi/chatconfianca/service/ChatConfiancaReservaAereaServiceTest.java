package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.chatconfianca.dto.reserva.ReservaAereaRecenteItem;
import com.confApi.chatconfianca.dto.reserva.ReservasAereasRecentesResponse;
import com.confApi.chatgpt.dto.ChatActionDTO;
import com.confApi.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatConfiancaReservaAereaServiceTest {
    private ChatConfiancaManagerClient manager;
    private ChatConfiancaReservaAereaService service;

    @BeforeEach
    void setUp() {
        manager = mock(ChatConfiancaManagerClient.class);
        service = new ChatConfiancaReservaAereaService(manager);
    }

    @Test
    void deveConsultarDezReservasDaAgenciaSemFiltroDeUsuarioOuStatus() {
        ReservaAereaRecenteItem item = reserva("ABC123", 91, true, true, true);
        when(manager.getList(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.<org.springframework.core.ParameterizedTypeReference<List<ReservaAereaRecenteItem>>>any()))
                .thenReturn(List.of(item));

        ReservasAereasRecentesResponse response = service.listarRecentes(321, null);

        verify(manager).getList(
                ArgumentMatchers.eq("chat-confianca/consultas/reservas-aereas/recentes"
                        + "?codgAgencia=321&limit=10"),
                ArgumentMatchers.<org.springframework.core.ParameterizedTypeReference<List<ReservaAereaRecenteItem>>>any());
        assertEquals(1, response.getQuantidade());
        assertEquals(List.of(
                        "abrir_reserva",
                        "simular_remarcacao",
                        "preparar_cancelamento",
                        "consultar_regras"),
                item.getActions().stream().map(ChatActionDTO::code).toList());
        assertEquals(91, item.getActions().get(0).reservaId());
        assertTrue(item.getActions().get(2).sensitive());
        assertTrue(item.getActions().get(2).requiresRules());
        assertTrue(item.getActions().get(2).requiresConfirmation());
        assertTrue(item.isEmissaoCandidata());
        assertFalse(item.getActions().stream()
                .anyMatch(action -> "preparar_emissao".equals(action.code())));
    }

    @Test
    void deveOmitirAcoesCondicionaisQuandoReservaNaoForAplicavel() {
        ReservaAereaRecenteItem item = reserva("XYZ789", 92, false, false, false);
        when(manager.getList(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.<org.springframework.core.ParameterizedTypeReference<List<ReservaAereaRecenteItem>>>any()))
                .thenReturn(List.of(item));

        service.listarRecentes(321, 5);

        assertEquals(List.of("abrir_reserva", "consultar_regras"),
                item.getActions().stream().map(ChatActionDTO::code).toList());
    }

    @Test
    void deveValidarAgenciaELimiteAntesDeConsultarManager() {
        assertThrows(RegraDeNegocioException.class, () -> service.listarRecentes(null, 10));
        assertThrows(RegraDeNegocioException.class, () -> service.listarRecentes(0, 10));
        assertThrows(RegraDeNegocioException.class, () -> service.listarRecentes(321, 0));
        assertThrows(RegraDeNegocioException.class, () -> service.listarRecentes(321, 51));
        verifyNoInteractions(manager);
    }

    private ReservaAereaRecenteItem reserva(String localizador,
                                             Integer reservaId,
                                             boolean remarcacao,
                                             boolean cancelamento,
                                             boolean emissao) {
        ReservaAereaRecenteItem item = new ReservaAereaRecenteItem();
        item.setReservaId(reservaId);
        item.setLocalizador(localizador);
        item.setDisponivelSimulacao(remarcacao);
        item.setCancelamentoRequerValidacao(cancelamento);
        item.setEmissaoCandidata(emissao);
        return item;
    }
}
