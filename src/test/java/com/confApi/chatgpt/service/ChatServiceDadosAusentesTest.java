package com.confApi.chatgpt.service;

import com.confApi.aereo.AereoClient;
import com.confApi.aereo.AereoRegrasReservaService;
import com.confApi.chatconfianca.service.ChatConfiancaReservaAereaService;
import com.confApi.chatgpt.config.OpenAIProperties;
import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.tools.ToolRouter;
import com.confApi.db.confManager.alertaTarifa.AlertaTarifaService;
import com.confApi.db.confManager.chatMemoria.ChatMemoriaService;
import com.confApi.db.confManager.chatMemoria.dto.ChatMemoria;
import com.confApi.db.confManager.familia.FamiliaService;
import com.confApi.db.confManager.faturas.FaturasService;
import com.confApi.db.wooba.checkin.CheckinService;
import com.confApi.hub.limites.LimitesService;
import java.util.*;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatServiceDadosAusentesTest {
    private final ChatMemoriaService memoria = mock(ChatMemoriaService.class);
    private final OkHttpClient client = mock(OkHttpClient.class);
    private final ChatService service = new ChatService(client, mock(OpenAIProperties.class),
            mock(ToolRouter.class), memoria, mock(LimitesService.class), mock(FaturasService.class),
            mock(CheckinService.class), mock(FamiliaService.class), mock(AlertaTarifaService.class),
            mock(ChatConfiancaReservaAereaService.class), mock(AereoClient.class), mock(AereoRegrasReservaService.class)) {
        @Override public String conversationAgentIA(String input) { return null; }
    };
    @Test void classificadorEMemoriasNulosNaoCausamNullPointer() {
        when(memoria.findByBase("Confianca")).thenReturn(null);
        List<String> resultado = service.actionApis(new ArrayList<>(), request(null));
        assertTrue(resultado.contains("desconhecido"));
        verifyNoInteractions(client);
    }
    @Test void listaImutavelDeKeywordsEMemoriasIncompletasSaoAceitas() {
        ChatMemoria vazia = new ChatMemoria();
        ChatMemoria valida = new ChatMemoria(); valida.setText("Conteudo valido");
        when(memoria.findByBase("Confianca")).thenReturn(Arrays.asList(null, vazia, valida));
        List<ChatMessageDTO> mensagens = new ArrayList<>();
        List<String> original = List.of();
        assertTrue(service.actionApis(mensagens, request(original)).contains("desconhecido"));
        assertEquals(1, mensagens.size());
        assertTrue(mensagens.get(0).content().contains("Conteudo valido"));
        assertTrue(original.isEmpty());
        verifyNoInteractions(client);
    }
    private ConversationRequestDTO request(List<String> keywords) {
        return new ConversationRequestDTO("confia", "Confianca", "ERP", 321L, 101L,
                "Tenho uma duvida especifica", new ArrayList<>(), null, false, keywords);
    }
}
