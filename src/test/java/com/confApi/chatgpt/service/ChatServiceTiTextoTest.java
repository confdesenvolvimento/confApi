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
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import okio.Buffer;
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatServiceTiTextoTest {
    final ToolRouter router=mock(ToolRouter.class);
    final AtomicInteger calls=new AtomicInteger();
    final AtomicLong timeoutNanos=new AtomicLong();
    final List<String> payloads=new ArrayList<>();
    ChatService service(String body,int status) {
        var client=new OkHttpClient.Builder().addInterceptor(chain->{
            calls.incrementAndGet();timeoutNanos.set(chain.call().timeout().timeoutNanos());
            var buffer=new Buffer();chain.request().body().writeTo(buffer);payloads.add(buffer.readUtf8());
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(status).message("mock").body(ResponseBody.create(body,MediaType.get("application/json"))).build();
        }).build();
        var props=mock(OpenAIProperties.class);when(props.getBaseUrl()).thenReturn("http://provider.invalid");
        when(props.getChatModel()).thenReturn("modelo-configurado");
        return new ChatService(client,props,router,mock(ChatMemoriaService.class),mock(LimitesService.class),
            mock(FaturasService.class),mock(CheckinService.class),mock(FamiliaService.class),mock(AlertaTarifaService.class),
            mock(ChatConfiancaReservaAereaService.class),mock(AereoClient.class),mock(AereoRegrasReservaService.class));
    }
    List<ChatMessageDTO> messages(){return List.of(new ChatMessageDTO("user","Qual contato de TI?"));}
    @Test void textoLimitadoUmaChamadaSemTrocaDeModeloNemFerramentas() throws Exception {
        var s=service("""
            {"id":"mock","choices":[{"finish_reason":"stop","message":{"role":"assistant","content":"Contato de TI."}}]}
            """,200);
        var r=s.responderTiSomenteTexto(messages());assertEquals("Contato de TI.",r.content());assertEquals(1,calls.get());
        var json=new ObjectMapper().readTree(payloads.get(0));
        assertEquals("modelo-configurado",json.path("model").asText());assertEquals(1200,json.path("max_completion_tokens").asInt());
        assertFalse(json.path("store").asBoolean(true));assertFalse(json.has("tools"));assertFalse(json.has("metadata"));
        assertEquals(15_000_000_000L,timeoutNanos.get());verifyNoInteractions(router);
    }
    @Test void ferramentaMesmoComFinishStopNaoExecuta() {
        var s=service("""
            {"choices":[{"finish_reason":"stop","message":{"content":null,
             "tool_calls":[{"id":"x","type":"function","function":{"name":"search_flights","arguments":"{}"}}]}}]}
            """,200);
        assertThrows(IOException.class,()->s.responderTiSomenteTexto(messages()));assertEquals(1,calls.get());verifyNoInteractions(router);
    }
    @Test void chamadaAntigaDeFuncaoTambemNaoExecuta() {
        var s=service("""
            {"choices":[{"finish_reason":"stop","message":{"content":"x","function_call":{"name":"search_flights","arguments":"{}"}}}]}
            """,200);
        assertThrows(IOException.class,()->s.responderTiSomenteTexto(messages()));verifyNoInteractions(router);
    }
    @Test void respostaTruncadaRecusaOuErroNaoSaoAceitos() {
        for(String body:List.of(
                "{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"incompleta\"}}]}",
                "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":null,\"refusal\":\"recusa\"}}]}",
                "{\"choices\":[]}")) {
            assertThrows(IOException.class,()->service(body,200).responderTiSomenteTexto(messages()));
        }
        assertThrows(IOException.class,()->service("{\"erro\":\"PRIVADO\"}",503).responderTiSomenteTexto(messages()));
        assertEquals(4,calls.get());verifyNoInteractions(router);
    }
    @Test void hotelTambemGeraSomenteTextoSemFerramentas() throws Exception {
        var s=service("{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"Hotel cadastrado.\"}}]}",200);
        assertEquals("Hotel cadastrado.",s.responderHotelSomenteTexto(messages()).content());
        var json=new ObjectMapper().readTree(payloads.get(0));assertFalse(json.has("tools"));
        assertFalse(json.path("store").asBoolean(true));assertEquals(15_000_000_000L,timeoutNanos.get());verifyNoInteractions(router);
    }
    @Test void hotelNaoExecutaToolSolicitadaPeloModelo() {
        var s=service("{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":null,\"tool_calls\":[{\"id\":\"1\",\"function\":{\"name\":\"search_hotels\",\"arguments\":\"{}\"}}]}}]}",200);
        assertThrows(IOException.class,()->s.responderHotelSomenteTexto(messages()));verifyNoInteractions(router);
    }
    @Test void caminhoAtualNaoRecebeParametrosDoPiloto() throws Exception {
        var s=service("{\"choices\":[{\"message\":{\"content\":\"Resposta atual\"}}]}",200);
        s.chat(new ChatRequestDTO(messages(),null,false,List.of(),Map.of()),List.of(),null);
        var json=new ObjectMapper().readTree(payloads.get(0));
        assertFalse(json.has("max_completion_tokens"));assertFalse(json.has("store"));
    }
}
