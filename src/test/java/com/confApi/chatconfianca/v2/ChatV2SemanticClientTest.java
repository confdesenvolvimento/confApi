package com.confApi.chatconfianca.v2;

import com.confApi.chatgpt.config.OpenAIProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import okio.Buffer;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatV2SemanticClientTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private final AtomicReference<JsonNode> sent=new AtomicReference<>();
    private ChatV2SemanticClient client(int status,String body) {
        OkHttpClient http=new OkHttpClient.Builder().addInterceptor(chain->{
            Buffer buffer=new Buffer();chain.request().body().writeTo(buffer);
            sent.set(mapper.readTree(buffer.readUtf8()));
            assertEquals("/v1/chat/completions",chain.request().url().encodedPath());
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(status).message("mock").body(ResponseBody.create(body,MediaType.get("application/json"))).build();
        }).build();
        OpenAIProperties ai=mock(OpenAIProperties.class);
        when(ai.getChatModel()).thenReturn("modelo-configurado");
        when(ai.getBaseUrl()).thenReturn("https://provider.invalid/");
        return new ChatV2SemanticClient(http,ai,new ChatV2Properties(),mapper);
    }
    private String plan(Map<String,Object> params)throws Exception {
        Map<String,Object> p=new LinkedHashMap<>();p.put("intencao",ChatV2Capability.FATURAS.code);
        Map<String,Object> complete=new LinkedHashMap<>();ChatV2SemanticClient.PARAMS.forEach(k->complete.put(k,null));complete.putAll(params);
        p.put("continuar",true);p.put("pergunta",null);p.put("parametros",complete);
        return mapper.writeValueAsString(p);
    }
    private String envelope(String content,String finish)throws Exception {
        return mapper.writeValueAsString(Map.of("choices",List.of(Map.of("finish_reason",finish,"message",Map.of("content",content)))));
    }
    @Test void enviaSchemaEstritoSemIdentidadeOuDadosFinanceiros()throws Exception {
        ChatV2SemanticClient client=client(200,envelope(plan(Map.of("modalidade","FATURADO")),"stop"));
        ChatV2Plan ctx=ChatV2Plan.of(ChatV2Capability.FATURAS);
        ctx.setAgencia(123456);ctx.setUsuario(654321);ctx.setResultado("DADOS_CONSULTADOS");
        ChatV2Plan p=client.decidir("Como pego ela em PDF?",ctx,LocalDate.of(2026,9,9));
        assertEquals(ChatV2Capability.FATURAS,p.capability());assertEquals("V2_SEMANTICA",p.getFonte());
        assertEquals("modelo-configurado",sent.get().path("model").asText());
        JsonNode format=sent.get().path("response_format").path("json_schema");
        assertTrue(format.path("strict").asBoolean());
        assertFalse(format.path("schema").path("additionalProperties").asBoolean());
        assertEquals(25,format.path("schema").path("properties").path("intencao").path("enum").size());
        assertFalse(sent.get().has("tools"));
        assertFalse(sent.get().toString().contains("123456"));assertFalse(sent.get().toString().contains("654321"));
    }
    @Test void negaIdentificadorDeAgenciaNoPlano()throws Exception {
        ChatV2SemanticClient client=client(200,"{}");
        assertThrows(IOException.class,()->client.parse(plan(Map.of("codgAgencia","999"))));
    }
    @Test void negaFerramentaOuIntencaoArbitraria()throws Exception {
        ChatV2SemanticClient client=client(200,"{}");
        assertThrows(IOException.class,()->client.parse(plan(Map.of()).replace(ChatV2Capability.FATURAS.code,"executar_sql")));
    }
    @Test void negaTipoEParametroLongo()throws Exception {
        ChatV2SemanticClient client=client(200,"{}");
        assertThrows(IOException.class,()->client.parse(plan(Map.of("adt",3))));
        assertThrows(IOException.class,()->client.parse(plan(Map.of("origem","A".repeat(1501)))));
    }
    @Test void falhaHttpNaoGeraPlano()throws Exception {
        ChatV2SemanticClient client=client(429,"{}");
        assertThrows(IOException.class,()->client.decidir("pedido",null,LocalDate.now()));
    }
    @Test void respostaTruncadaNaoGeraPlano()throws Exception {
        ChatV2SemanticClient client=client(200,envelope(plan(Map.of()),"length"));
        assertThrows(IOException.class,()->client.decidir("pedido",null,LocalDate.now()));
    }
    @Test void recusaNaoGeraPlano()throws Exception {
        String body=mapper.writeValueAsString(Map.of("choices",List.of(Map.of("finish_reason","stop","message",Map.of("refusal","Recusa")))));
        ChatV2SemanticClient client=client(200,body);
        assertThrows(IOException.class,()->client.decidir("pedido",null,LocalDate.now()));
    }
    @Test void objetoNuloNaoGeraPlano() {
        ChatV2SemanticClient client=client(200,"null");
        assertThrows(IOException.class,()->client.decidir("pedido",null,LocalDate.now()));
    }
}
