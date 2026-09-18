package com.confApi.chatgpt.service;

import com.confApi.aereo.*;
import com.confApi.chatconfianca.service.ChatConfiancaReservaAereaService;
import com.confApi.chatgpt.config.OpenAIProperties;
import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.tools.ToolRouter;
import com.confApi.db.confManager.alertaTarifa.AlertaTarifaService;
import com.confApi.db.confManager.chatMemoria.ChatMemoriaService;
import com.confApi.db.confManager.familia.FamiliaService;
import com.confApi.db.confManager.faturas.FaturasService;
import com.confApi.db.wooba.checkin.CheckinService;
import com.confApi.hub.limites.LimitesService;
import okhttp3.*;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatServiceV2ToolGuardTest {
    @Test void resumidorNaoPodeInventarOutraExecucao() {
        OkHttpClient client=new OkHttpClient.Builder().addInterceptor(chain->new Response.Builder()
                .request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("mock")
                .body(ResponseBody.create("""
                    {"id":"resposta","choices":[{"finish_reason":"tool_calls","message":{"role":"assistant","content":null,
                    "tool_calls":[{"id":"nao-autorizada","type":"function","function":{"name":"search_flights","arguments":"{}"}}]}}]}
                    """,MediaType.get("application/json"))).build()).build();
        OpenAIProperties props=mock(OpenAIProperties.class);
        when(props.getBaseUrl()).thenReturn("https://provider.invalid");when(props.getChatModel()).thenReturn("mock");
        ToolRouter router=mock(ToolRouter.class);
        ChatService service=new ChatService(client,props,router,mock(ChatMemoriaService.class),mock(LimitesService.class),
                mock(FaturasService.class),mock(CheckinService.class),mock(FamiliaService.class),mock(AlertaTarifaService.class),
                mock(ChatConfiancaReservaAereaService.class),mock(AereoClient.class),mock(AereoRegrasReservaService.class));
        ChatRequestDTO request=new ChatRequestDTO(List.of(new ChatMessageDTO("user","Resuma os dados.")),null,false,List.of(),Map.of("coordenadorV2",true));
        assertThrows(IOException.class,()->service.chat(request,List.of(),null));
        verifyNoInteractions(router);
    }
}
