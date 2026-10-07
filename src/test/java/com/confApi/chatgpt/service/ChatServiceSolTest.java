package com.confApi.chatgpt.service;

import com.confApi.aereo.*;
import com.confApi.chatconfianca.service.ChatConfiancaReservaAereaService;
import com.confApi.chatgpt.config.OpenAIProperties;
import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.tools.*;
import com.confApi.db.confManager.alertaTarifa.AlertaTarifaService;
import com.confApi.db.confManager.chatMemoria.ChatMemoriaService;
import com.confApi.db.confManager.familia.FamiliaService;
import com.confApi.db.confManager.faturas.FaturasService;
import com.confApi.db.wooba.checkin.CheckinService;
import com.confApi.hub.limites.LimitesService;
import com.fasterxml.jackson.databind.*;
import okhttp3.*;
import okio.Buffer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ChatServiceSolTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final OpenAIProperties props = new OpenAIProperties();

    @org.junit.jupiter.api.BeforeEach void configurarModeloDoTeste() {
        props.setChatModel("gpt-6.1-sol");
    }
    private final ToolRouter tools = mock(ToolRouter.class);
    private final List<JsonNode> sent = Collections.synchronizedList(new ArrayList<>());
    private final List<String> paths = Collections.synchronizedList(new ArrayList<>());
    private final List<String> responses = new ArrayList<>();
    private final AtomicInteger calls = new AtomicInteger();
    private int status = 200;

    private ChatService service() {
        props.setBaseUrl("https://provider.invalid/");
        props.setApiKey("unused-test");
        OkHttpClient http = new OkHttpClient.Builder().addInterceptor(chain -> {
            Buffer buffer = new Buffer(); chain.request().body().writeTo(buffer);
            sent.add(mapper.readTree(buffer.readUtf8())); paths.add(chain.request().url().encodedPath());
            int i = calls.getAndIncrement();
            String body = responses.get(Math.min(i, responses.size() - 1));
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(status).message("mock").body(ResponseBody.create(body, MediaType.get("application/json"))).build();
        }).build();
        return new ChatService(http, props, tools, mock(ChatMemoriaService.class), mock(LimitesService.class),
                mock(FaturasService.class), mock(CheckinService.class), mock(FamiliaService.class),
                mock(AlertaTarifaService.class), mock(ChatConfiancaReservaAereaService.class),
                mock(AereoClient.class), mock(AereoRegrasReservaService.class));
    }

    private ChatRequestDTO request(String model, List<ToolDefinition> definitions) {
        return new ChatRequestDTO(List.of(new ChatMessageDTO("system", "Orientacao de teste."),
                new ChatMessageDTO("user", "Qual o contato do financeiro?")), model, false, definitions,
                Map.of("coordenadorV2", true));
    }

    private ToolDefinition definition(String name) {
        return new ToolDefinition(name, "Consulta autorizada.", Map.of("type", "object", "properties",
                Map.of("filtro", Map.of("type", "string")), "required", List.of()));
    }

    private String text(String value) throws Exception {
        return envelope(List.of(Map.of("type", "message", "role", "assistant", "status", "completed",
                "content", List.of(Map.of("type", "output_text", "text", value)))));
    }

    private Map<String,Object> tool(String name, String callId, String arguments) {
        // The API schema permits an omitted function-call status. call_id differs from the output-item id.
        return Map.of("type", "function_call", "id", "fc_" + callId, "call_id", callId,
                "name", name, "arguments", arguments);
    }

    private String envelope(List<?> output) throws Exception {
        return mapper.writeValueAsString(Map.of("id", "resp_test", "status", "completed", "output", output));
    }

    @Test void defaultSolUsaResponsesComRaciocinioBaixoEHistoricoLocal() throws Exception {
        responses.add(text("Contato confirmado."));
        ChatResponseDTO result = service().chat(request(null, List.of()), List.of("contatos"),
                List.of(new ChatMessageDTO("user", "Preciso de ajuda.")));
        assertEquals("gpt-6.1-sol", props.getChatModel());
        assertEquals("/v1/responses", paths.get(0));
        JsonNode payload = sent.get(0);
        assertEquals("low", payload.path("reasoning").path("effort").asText());
        assertEquals(8192, payload.path("max_output_tokens").asInt());
        assertFalse(payload.path("store").asBoolean(true));
        assertEquals("reasoning.encrypted_content", payload.path("include").path(0).asText());
        assertEquals(3, payload.path("input").size());
        assertFalse(payload.has("messages")); assertFalse(payload.has("previous_response_id"));
        assertFalse(payload.has("tools")); assertFalse(payload.has("temperature"));
        assertEquals("Contato confirmado.", result.content());
        assertEquals("resp_test", result.id()); assertEquals(4, result.history().size());
        assertEquals(List.of("contatos"), result.keywords()); verifyNoInteractions(tools);
    }

    @Test void preservaRaciocinioECallIdNoRetornoDaFerramenta() throws Exception {
        responses.add(envelope(List.of(
                Map.of("type", "reasoning", "id", "rs_test", "summary", List.of(), "encrypted_content", "opaque-test"),
                tool("consultar_teste", "call_test", "{\"filtro\":\"ABERTO\"}"))));
        responses.add(text("Consulta concluida."));
        when(tools.execute(eq("consultar_teste"), any())).thenReturn(Map.of("status", "OK", "quantidade", 2));
        ChatResponseDTO result = service().chat(request(null, List.of(definition("consultar_teste"))), List.of(), null);
        assertEquals(2, calls.get());
        JsonNode first = sent.get(0);
        assertFalse(first.path("tools").path(0).has("function"));
        assertEquals("consultar_teste", first.path("tools").path(0).path("name").asText());
        assertFalse(first.path("tools").path(0).path("strict").asBoolean(true));
        assertFalse(first.path("parallel_tool_calls").asBoolean(true));
        JsonNode input = sent.get(1).path("input");
        assertEquals(5, input.size());
        assertEquals("reasoning", input.path(2).path("type").asText());
        assertEquals("opaque-test", input.path(2).path("encrypted_content").asText());
        assertEquals("function_call", input.path(3).path("type").asText());
        assertEquals("function_call_output", input.path(4).path("type").asText());
        assertEquals("call_test", input.path(4).path("call_id").asText());
        assertEquals(2, mapper.readTree(input.path(4).path("output").asText()).path("quantidade").asInt());
        assertFalse(result.history().toString().contains("opaque-test"));
        assertEquals("Consulta concluida.", result.content());
        verify(tools).execute("consultar_teste", Map.of("filtro", "ABERTO"));
    }

    @Test void tarifaMantemRespostaFactualEUmaUnicaChamada() throws Exception {
        responses.add(envelope(List.of(tool("search_cheapest_airfares", "call_fare", "{\"origem\":\"CGB\",\"destino\":\"BSB\"}"))));
        when(tools.execute(eq("search_cheapest_airfares"), any())).thenReturn(Map.of("status", "OK", "mensagem", "Menor tarifa: R$ 500,00."));
        ChatRequestDTO req = new ChatRequestDTO(List.of(new ChatMessageDTO("user", "Menor tarifa de CGB para BSB?")),
                null, false, List.of(ToolSchemas.searchCheapestAirfares()), Map.of());
        ChatResponseDTO result = service().chat(req, List.of(), null);
        assertEquals(1, calls.get());
        assertEquals("search_cheapest_airfares", sent.get(0).path("tool_choice").path("name").asText());
        assertFalse(sent.get(0).path("tool_choice").has("function"));
        assertEquals("Menor tarifa: R$ 500,00.", result.content());
        assertEquals(result.content(), result.history().get(result.history().size() - 1).content());
    }

    @Test void buscaHotelPreservaPayloadEstruturadoParaCliente() throws Exception {
        responses.add(envelope(List.of(tool("search_hotels", "call_hotel", "{}"))));
        responses.add(text("Pesquisa preparada."));
        when(tools.execute(eq("search_hotels"), any())).thenReturn(Map.of("status", "OK", "destino", "Manaus"));
        ChatResponseDTO result = service().chat(request(null, List.of(definition("search_hotels"))), List.of(), null);
        assertEquals("Manaus", mapper.readTree(result.content()).path("destino").asText());
        assertEquals(result.content(), result.history().get(result.history().size() - 1).content());
        assertEquals(1, result.toolCalls().size());
    }

    @Test void rollbackExplicitoPara4oMantemChatCompletions() throws Exception {
        responses.add("{\"id\":\"legacy\",\"choices\":[{\"message\":{\"content\":\"Resposta antiga.\"}}]}");
        ChatResponseDTO result = service().chat(request("gpt-4o", List.of()), List.of(), null);
        assertEquals("/v1/chat/completions", paths.get(0)); assertEquals("gpt-4o", sent.get(0).path("model").asText());
        assertTrue(sent.get(0).has("messages")); assertFalse(sent.get(0).has("reasoning"));
        assertEquals("Resposta antiga.", result.content());
    }

    @Test void modeloEmBrancoUsaDefaultConfigurado() throws Exception {
        responses.add(text("Resposta."));
        service().chat(request("  ", List.of()), List.of(), null);
        assertEquals("gpt-6.1-sol", sent.get(0).path("model").asText());
    }

    @Test void modeloAtualContinuaUsandoChatCompletionsPorPadrao() throws Exception {
        props.setChatModel(new OpenAIProperties().getChatModel());
        responses.add("{\"choices\":[{\"message\":{\"content\":\"Resposta.\"}}]}");
        service().chat(request(null, List.of()), List.of(), null);
        assertEquals("/v1/chat/completions", paths.get(0));
    }

    @Test void textoDeTiTemOrcamentoParaRaciocinioESemFerramentas() throws Exception {
        responses.add(text("Orientacao de TI."));
        ChatResponseDTO result = service().responderTiSomenteTexto(request(null, List.of()).messages());
        assertEquals(4096, sent.get(0).path("max_output_tokens").asInt());
        assertEquals("Orientacao de TI.", result.content()); assertEquals(1, calls.get()); verifyNoInteractions(tools);
    }

    @ParameterizedTest @ValueSource(strings = {"falhou", "incomplete", "in_progress"})
    void resultadoNaoConcluidoNaoExecutaFerramenta(String responseStatus) throws Exception {
        responses.add(envelope(List.of(tool("consultar_teste", "call_x", "{}"))).replace("\"status\":\"completed\"", "\"status\":\"" + responseStatus + "\""));
        assertThrows(IOException.class, () -> service().chat(request(null, List.of(definition("consultar_teste"))), List.of(), null));
        verifyNoInteractions(tools);
    }

    @Test void ferramentaNaoOferecidaNaoExecuta() throws Exception {
        responses.add(envelope(List.of(tool("executar_sql", "call_x", "{}"))));
        assertThrows(IOException.class, () -> service().chat(request(null, List.of(definition("consultar_teste"))), List.of(), null));
        verifyNoInteractions(tools);
    }

    @Test void validaTodasChamadasAntesDeExecutarQualquerUma() throws Exception {
        responses.add(envelope(List.of(tool("consultar_teste", "call_1", "{}"), tool("consultar_teste", "call_2", "[]"))));
        assertThrows(IOException.class, () -> service().chat(request(null, List.of(definition("consultar_teste"))), List.of(), null));
        verifyNoInteractions(tools);
    }

    @Test void callIdRepetidoNaoExecutaNovamente() throws Exception {
        responses.add(envelope(List.of(tool("consultar_teste", "call_1", "{}"))));
        when(tools.execute(eq("consultar_teste"), any())).thenReturn(Map.of("status", "OK"));
        assertThrows(IOException.class, () -> service().chat(request(null, List.of(definition("consultar_teste"))), List.of(), null));
        assertEquals(2, calls.get()); verify(tools, times(1)).execute(eq("consultar_teste"), any());
    }

    @Test void cicloDeFerramentasTemLimiteDeOitoRequisicoes() throws Exception {
        for (int i = 0; i < 8; i++) responses.add(envelope(List.of(tool("consultar_teste", "call_" + i, "{}"))));
        when(tools.execute(eq("consultar_teste"), any())).thenReturn(Map.of("status", "OK"));
        IOException error = assertThrows(IOException.class, () -> service().chat(request(null, List.of(definition("consultar_teste"))), List.of(), null));
        assertEquals("OPENAI_TOOL_ROUNDS_EXCEDIDOS", error.getMessage()); assertEquals(8, calls.get());
    }

    @ParameterizedTest @ValueSource(strings = {"{\"status\":\"completed\",\"output\":[]}", "null", "{", "{\"error\":{\"message\":\"privado\"}}"})
    void corpoAusenteInvalidoOuVazioGeraErroControlado(String body) {
        responses.add(body);
        assertThrows(IOException.class, () -> service().chat(request(null, List.of()), List.of(), null)); verifyNoInteractions(tools);
    }

    @Test void erroHttpNaoExpoeCorpoDaOpenAI() {
        status = 429; responses.add("{\"error\":{\"message\":\"conteudo privado\"}}");
        IOException error = assertThrows(IOException.class, () -> service().chat(request(null, List.of()), List.of(), null));
        assertEquals("OPENAI_RESPONSES_HTTP_429", error.getMessage()); verifyNoInteractions(tools);
    }

    @Test void recusaNaoViraRespostaOuConsultaConcluida() throws Exception {
        responses.add(envelope(List.of(Map.of("type", "message", "role", "assistant", "status", "completed",
                "content", List.of(Map.of("type", "refusal", "refusal", "Recusa."))))));
        assertThrows(IOException.class, () -> service().chat(request(null, List.of()), List.of(), null)); verifyNoInteractions(tools);
    }

    @Test void parametrosDeRaciocinioInvalidosFalhamAntesDoHttp() {
        props.setChatReasoningEffort("none"); responses.add("{}");
        assertThrows(IOException.class, () -> service().chat(request(null, List.of()), List.of(), null));
        assertEquals(0, calls.get());
    }

    @Test void streamingMantemFormatoDeEventosAtualComSol() {
        String chunk = "{\"choices\":[{\"delta\":{\"content\":\"Ola\"}}]}";
        responses.add("data: " + chunk + "\n\ndata: [DONE]\n\n");
        List<String> result = service().stream(request(null, List.of())).collectList().block(Duration.ofSeconds(5));
        assertEquals(List.of(chunk), result); assertEquals("/v1/chat/completions", paths.get(0));
        assertEquals("gpt-6.1-sol", sent.get(0).path("model").asText());
        assertEquals("low", sent.get(0).path("reasoning_effort").asText());
        assertTrue(sent.get(0).path("stream").asBoolean()); assertFalse(sent.get(0).has("tools"));
    }
}
