package com.confApi.chatgpt.service;

import com.confApi.aereo.AereoClient;
import com.confApi.aereo.AereoRegrasReservaService;
import com.confApi.chatconfianca.service.ChatConfiancaReservaAereaService;
import com.confApi.chatgpt.config.OpenAIProperties;
import com.confApi.chatgpt.dto.ChatMessageDTO;
import com.confApi.chatgpt.dto.ChatRequestDTO;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.confApi.chatgpt.tools.ToolRouter;
import com.confApi.chatgpt.tools.ToolSchemas;
import com.confApi.db.confManager.alertaTarifa.AlertaTarifaService;
import com.confApi.db.confManager.chatMemoria.ChatMemoriaService;
import com.confApi.db.confManager.familia.FamiliaService;
import com.confApi.db.confManager.faturas.FaturasService;
import com.confApi.db.wooba.checkin.CheckinService;
import com.confApi.endPoints.reservaAereo.ReservaAereoApi;
import com.confApi.hub.limites.LimitesService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceToolProtocolTest {

    @Test
    void finalizaTarifaLocalmenteENaoHerdaFiltrosQuandoRotaMuda() throws Exception {
        Execucao execucao = executar(
                Map.of(
                        "origem", "CGB",
                        "destino", "MCO",
                        "modoResposta", "alternativas"),
                Map.of(
                        "origem", "GRU",
                        "destino", "BSB",
                        "cabine", "C",
                        "periodoInicio", "2027-01-01",
                        "periodoFim", "2027-01-31",
                        "limiteAlternativas", 7),
                "Qual a menor tarifa de CGB para MCO?");

        assertThat(execucao.chamadas()).isEqualTo(1);
        assertThat(execucao.primeiroPayload().path("tool_choice")
                .path("function").path("name").asText())
                .isEqualTo("search_cheapest_airfares");
        assertThat(execucao.primeiroPayload().toString())
                .doesNotContain("contextoLocalMelhoresTarifasAereas", "GRU", "BSB");
        assertThat(execucao.response().content())
                .isEqualTo("A menor tarifa e R$ 3.500,60.");
        assertThat(execucao.argumentos())
                .containsEntry("origem", "CGB")
                .containsEntry("destino", "MCO")
                .containsEntry("modoResposta", "alternativas")
                .doesNotContainKeys(
                        "cabine", "dataInicio", "dataFim", "limiteAlternativas");
    }

    @Test
    void reaproveitaFiltrosQuandoRotaExplicitaPermaneceIgual() throws Exception {
        Execucao execucao = executar(
                Map.of(
                        "origem", "CGB",
                        "destino", "BSB",
                        "modoResposta", "alternativas"),
                Map.of(
                        "origem", "CGB",
                        "destino", "BSB",
                        "cabine", "C",
                        "periodoInicio", "2027-01-01",
                        "periodoFim", "2027-01-31",
                        "limiteAlternativas", 7),
                "Mostre outras datas de CGB para BSB");

        assertThat(execucao.chamadas()).isEqualTo(1);
        assertThat(execucao.argumentos())
                .containsEntry("origem", "CGB")
                .containsEntry("destino", "BSB")
                .containsEntry("cabine", "C")
                .containsEntry("dataInicio", "2027-01-01")
                .containsEntry("dataFim", "2027-01-31")
                .containsEntry("limiteAlternativas", 7);
    }

    @Test
    void finalizaIdaVoltaLocalmenteEReaproveitaContextoEstruturado() throws Exception {
        Execucao execucao = executar(
                Map.of("modoResposta", "companhias"),
                Map.ofEntries(
                        Map.entry("tipoViagem", "ida_volta"),
                        Map.entry("origem", "CGB"),
                        Map.entry("destino", "MCO"),
                        Map.entry("cabine", "C"),
                        Map.entry("dataIdaInicio", "2027-02-01"),
                        Map.entry("dataIdaFim", "2027-02-28"),
                        Map.entry("dataVoltaInicio", "2027-03-01"),
                        Map.entry("dataVoltaFim", "2027-03-31"),
                        Map.entry("duracaoMinimaDias", 5),
                        Map.entry("duracaoMaximaDias", 12),
                        Map.entry("politicaCompanhia", "comparar")),
                "E em fevereiro na executiva?",
                "search_cheapest_roundtrip_airfares");

        assertThat(execucao.chamadas()).isEqualTo(1);
        assertThat(execucao.primeiroPayload().path("tool_choice")
                .path("function").path("name").asText())
                .isEqualTo("search_cheapest_roundtrip_airfares");
        assertThat(execucao.response().content())
                .isEqualTo("O menor total combinado e R$ 5.000,00.");
        assertThat(execucao.argumentos())
                .containsEntry("origem", "CGB")
                .containsEntry("destino", "MCO")
                .containsEntry("cabine", "C")
                .containsEntry("dataIdaInicio", "2027-02-01")
                .containsEntry("dataIdaFim", "2027-02-28")
                .containsEntry("dataVoltaInicio", "2027-03-01")
                .containsEntry("dataVoltaFim", "2027-03-31")
                .containsEntry("duracaoMinimaDias", 5)
                .containsEntry("duracaoMaximaDias", 12)
                .containsEntry("politicaCompanhia", "comparar");
    }

    @Test
    void forcaIdaVoltaAoEncontrarRotaEmUserAnteriorSemMetadata() throws Exception {
        List<ChatMessageDTO> mensagens = List.of(
                new ChatMessageDTO("system", "Prompt de teste."),
                new ChatMessageDTO("user",
                        "Qual a menor tarifa ida e volta de CGB para MCO?"),
                new ChatMessageDTO("assistant",
                        "Para 1 adulto, ida e volta: total combinado de R$ 5.000,00."),
                new ChatMessageDTO("user", "E em fevereiro?"));

        Execucao execucao = executar(
                Map.of(
                        "origem", "CGB",
                        "destino", "MCO",
                        "mesIda", "2027-02"),
                mensagens,
                Map.of(),
                "search_cheapest_roundtrip_airfares");

        assertThat(execucao.chamadas()).isEqualTo(1);
        assertThat(execucao.primeiroPayload().path("tool_choice")
                .path("function").path("name").asText())
                .isEqualTo("search_cheapest_roundtrip_airfares");
        assertThat(execucao.argumentos())
                .containsEntry("origem", "CGB")
                .containsEntry("destino", "MCO")
                .containsEntry("mesIda", "2027-02");
    }

    @Test
    void idaVoltaNaoHerdaFiltrosQuandoRotaMuda() throws Exception {
        Execucao execucao = executar(
                Map.of("origem", "CGB", "destino", "BSB"),
                Map.ofEntries(
                        Map.entry("tipoViagem", "ida_volta"),
                        Map.entry("origem", "CGB"),
                        Map.entry("destino", "MCO"),
                        Map.entry("cabine", "C"),
                        Map.entry("dataIdaInicio", "2027-02-01"),
                        Map.entry("dataIdaFim", "2027-02-28"),
                        Map.entry("dataVoltaInicio", "2027-03-01"),
                        Map.entry("dataVoltaFim", "2027-03-31"),
                        Map.entry("duracaoMinimaDias", 5),
                        Map.entry("duracaoMaximaDias", 12),
                        Map.entry("politicaCompanhia", "mesma")),
                "E de CGB para BSB?",
                "search_cheapest_roundtrip_airfares");

        assertThat(execucao.chamadas()).isEqualTo(1);
        assertThat(execucao.argumentos())
                .containsEntry("origem", "CGB")
                .containsEntry("destino", "BSB")
                .doesNotContainKeys(
                        "cabine", "dataIdaInicio", "dataIdaFim",
                        "dataVoltaInicio", "dataVoltaFim",
                        "duracaoMinimaDias", "duracaoMaximaDias",
                        "politicaCompanhia", "limiteAlternativas");
    }

    private Execucao executar(Map<String, Object> argumentosModelo,
                              Map<String, Object> contextoLocal,
                              String mensagemUsuario) throws Exception {
        return executar(argumentosModelo, contextoLocal, mensagemUsuario,
                "search_cheapest_airfares");
    }

    private Execucao executar(Map<String, Object> argumentosModelo,
                              Map<String, Object> contextoLocal,
                              String mensagemUsuario,
                              String nomeFerramenta) throws Exception {
        return executar(
                argumentosModelo,
                List.of(new ChatMessageDTO("user", mensagemUsuario)),
                Map.of("contextoLocalMelhoresTarifasAereas", contextoLocal),
                nomeFerramenta);
    }

    private Execucao executar(Map<String, Object> argumentosModelo,
                              List<ChatMessageDTO> mensagens,
                              Map<String, Object> metadata,
                              String nomeFerramenta) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AtomicInteger chamadas = new AtomicInteger();
        AtomicReference<JsonNode> primeiroPayload = new AtomicReference<>();
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    int chamada = chamadas.incrementAndGet();
                    if (chamada > 1) {
                        throw new AssertionError(
                                "A tarifa local nao deve depender de uma segunda chamada OpenAI.");
                    }
                    Buffer body = new Buffer();
                    chain.request().body().writeTo(body);
                    primeiroPayload.set(mapper.readTree(body.readUtf8()));
                    Map<String, Object> function = Map.of(
                            "name", nomeFerramenta,
                            "arguments", mapper.writeValueAsString(argumentosModelo));
                    Map<String, Object> toolCall = Map.of(
                            "id", "call_tarifa",
                            "type", "function",
                            "function", function);
                    Map<String, Object> message = Map.of(
                            "role", "assistant",
                            "tool_calls", List.of(toolCall));
                    String json = mapper.writeValueAsString(Map.of(
                            "id", "chat-1",
                            "choices", List.of(Map.of("message", message))));
                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(ResponseBody.create(
                                    json,
                                    MediaType.get("application/json")))
                            .build();
                })
                .build();

        ToolRouter router = mock(ToolRouter.class);
        String mensagem = "search_cheapest_roundtrip_airfares".equals(nomeFerramenta)
                ? "O menor total combinado e R$ 5.000,00."
                : "A menor tarifa e R$ 3.500,60.";
        when(router.execute(eq(nomeFerramenta), any())).thenReturn(Map.of(
                "status", "OK",
                "mensagem", mensagem,
                "actions", List.of()));
        OpenAIProperties properties = mock(OpenAIProperties.class);
        when(properties.getChatModel()).thenReturn("test-model");
        when(properties.getBaseUrl()).thenReturn("http://localhost");
        ChatService service = new ChatService(
                client,
                properties,
                router,
                mock(ChatMemoriaService.class),
                mock(LimitesService.class),
                mock(FaturasService.class),
                mock(CheckinService.class),
                mock(FamiliaService.class),
                mock(AlertaTarifaService.class),
                mock(ChatConfiancaReservaAereaService.class),
                mock(AereoClient.class),
                mock(AereoRegrasReservaService.class),
                mock(ReservaAereoApi.class));

        ChatResponseDTO response = service.chat(
                new ChatRequestDTO(
                        mensagens,
                        null,
                        false,
                        List.of("search_cheapest_roundtrip_airfares".equals(nomeFerramenta)
                                ? ToolSchemas.searchCheapestRoundtripAirfares()
                                : ToolSchemas.searchCheapestAirfares()),
                        metadata),
                new ArrayList<>(),
                null);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Map> argumentos = ArgumentCaptor.forClass(Map.class);
        verify(router).execute(eq(nomeFerramenta), argumentos.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> argumentosExecutados = argumentos.getValue();
        return new Execucao(
                chamadas.get(), primeiroPayload.get(), response, argumentosExecutados);
    }

    private record Execucao(int chamadas,
                            JsonNode primeiroPayload,
                            ChatResponseDTO response,
                            Map<String, Object> argumentos) {
    }
}
