package com.confApi.chatgpt.service;

import com.confApi.chatgpt.config.OpenAIProperties;
import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.tools.ToolRouter;
import com.confApi.db.confManager.chatMemoria.ChatMemoriaService;
import com.confApi.db.confManager.chatMemoria.dto.ChatMemoria;
import com.confApi.db.confManager.faturas.FaturasService;
import com.confApi.db.confManager.faturas.dto.FaturaIA;
import com.confApi.db.confManager.faturas.dto.FaturaSicaRQ;
import com.confApi.db.confManager.faturas.dto.FaturaSicaRS;
import com.confApi.db.confManager.faturas.dto.model.FaturaResponseIA;
import com.confApi.hub.limites.LimitesService;
import com.confApi.hub.limites.dto.Disponibilidade;
import com.confApi.hub.limites.dto.LimiteCreditoRQ;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import okio.BufferedSource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

// ChatService.java
@Service
@RequiredArgsConstructor
public class ChatService {
    private final OkHttpClient client;
    private final OpenAIProperties props;
    private final ToolRouter tools;

    private final ChatMemoriaService chatMemoriaService;
    private final LimitesService limitesService;
    private final FaturasService faturasService;

    public ChatResponseDTO chat(ChatRequestDTO req) throws IOException {
        String model = Optional.ofNullable(req.model()).orElse(props.getChatModel());

        // monta body para /v1/chat/completions
        Map<String,Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", req.messages().stream()
                .map(m -> Map.of("role", m.role(), "content", m.content()))
                .toList());
        if (req.tools() != null && !req.tools().isEmpty()) {
            body.put("tools", req.tools().stream().map(td -> Map.of(
                    "type", "function",
                    "function", Map.of(
                            "name", td.name(),
                            "description", td.description(),
                            "parameters", td.jsonSchema()
                    ))).toList());
        }

        Request request = new Request.Builder()
                .url(props.getBaseUrl() + "/v1/chat/completions")
                .post(RequestBody.create(
                        MediaType.parse("application/json"),
                        new ObjectMapper().writeValueAsBytes(body)))
                .build();

        try (Response r = client.newCall(request).execute()) {
            String json = Objects.requireNonNull(r.body()).string();
            // parse simplificado
            JsonNode root = new ObjectMapper().readTree(json);
            JsonNode choice = root.path("choices").get(0);
            String content = choice.path("message").path("content").asText();

            // verifica se houve chamadas de ferramenta
            List<ToolCallDTO> toolCalls = new ArrayList<>();
            JsonNode tc = choice.path("message").path("tool_calls");
            if (tc.isArray()) {
                for (JsonNode n : tc) {
                    String name = n.path("function").path("name").asText();
                    String argsStr = n.path("function").path("arguments").asText("{}");
                    Map<String,Object> args = new ObjectMapper().readValue(argsStr, new TypeReference<>(){});
                    Map<String,Object> result = tools.execute(name, args);
                    toolCalls.add(new ToolCallDTO(name, result));
                }
            }

            return new ChatResponseDTO(root.path("id").asText(), content, toolCalls, null);
        }
    }

    public Flux<String> stream(ChatRequestDTO req) {
        // faz chamada SSE (stream=true) e emite os deltas como texto
        return Flux.create(sink -> {
            try {
                Map<String,Object> body = new HashMap<>();
                body.put("model", Optional.ofNullable(req.model()).orElse(props.getChatModel()));
                body.put("stream", true);
                body.put("messages", req.messages().stream()
                        .map(m -> Map.of("role", m.role(), "content", m.content()))
                        .toList());

                Request request = new Request.Builder()
                        .url(props.getBaseUrl() + "/v1/chat/completions")
                        .post(RequestBody.create(
                                MediaType.parse("application/json"),
                                new ObjectMapper().writeValueAsBytes(body)))
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) { sink.error(e); }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        try (ResponseBody rb = response.body()) {
                            BufferedSource src = rb.source();
                            while (!src.exhausted()) {
                                String line = src.readUtf8LineStrict();
                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6).trim();
                                    if ("[DONE]".equals(data)) break;
                                    sink.next(data);
                                }
                            }
                            sink.complete();
                        }
                    }
                });
            } catch (Exception ex) {
                sink.error(ex);
            }
        });
    }

    public void actionApis(List<ChatMessageDTO> messages, ConversationRequestDTO req){

        List<ChatMemoria> chatMemorias = chatMemoriaService.findByBase(req.unidade());
        for (ChatMemoria chtMemoria : chatMemorias){
            System.out.println("Memoria: "+chtMemoria.getText());
            messages.add(new ChatMessageDTO("system", "Dado do sistema: " + chtMemoria.getText()));
        }

        /*Consultar limites de credito*/
        System.out.println("Limite Erp: "+req.idErp());
        Disponibilidade limitesDisponiveis = limitesService.consultaLimiteApi(new LimiteCreditoRQ(req.idErp()));
        messages.add(new ChatMessageDTO("system", "Dado do sistema: " + limitesDisponiveis.gerarResumoLimites()));

        /* Consultar Faturas*/
        montarMensagemFaturas(req);
            messages.add(new ChatMessageDTO("system", "Dado do sistema: " + montarMensagemFaturas(req)));


    }

    public ChatMessageDTO montarMensagemFaturas(ConversationRequestDTO req) {
        // 1) Monta o request
        FaturaSicaRQ faturaSicaRQ = new FaturaSicaRQ();
        faturaSicaRQ.setInvoiceType("TODOS");
        faturaSicaRQ.setEmpfat(req.idErp());
        faturaSicaRQ.setTipoData("TODAS");
        faturaSicaRQ.setDataInicio(null);
        faturaSicaRQ.setDataFim(null);
        faturaSicaRQ.setPagamento("ABERTO");
        faturaSicaRQ.setDisabledAFaturar(false);

        // 2) Consulta o serviço
        List<FaturaSicaRS> faturas = Collections.emptyList();
        try {
            faturas = Optional.ofNullable(faturasService.faturaSica(faturaSicaRQ))
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            System.out.println("Erro ao consultar faturas no faturasService "+e);
        }

        // 3) Converte/normaliza datas (dd/MM/yyyy) com null-safety
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        for (FaturaSicaRS f : faturas) {
            try {
                if (f.getDataFatura() != null && !f.getDataFatura().isBlank()) {
                    f.setConvertDataFatura(formatter.parse(f.getDataFatura()));
                }
                if (f.getDataVen() != null && !f.getDataVen().isBlank()) {
                    f.setConvertDataVen(formatter.parse(f.getDataVen()));
                }
            } catch (ParseException pe) {
                System.out.println("Falha ao parsear datas da fatura: "+pe);

            }
        }

        // 4) Prepara ObjectMapper (um só), tolerante a campos extras
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 5) Mapeia diretamente a lista de FaturaSicaRS -> List<FaturaIA> sem serializar antes
        List<FaturaIA> faturasIA = mapper.convertValue(
                faturas,
                new TypeReference<List<FaturaIA>>() {}
        );

        // 6) Monta o wrapper de resposta
        FaturaResponseIA fResponse = new FaturaResponseIA();
        fResponse.setFaturas(
                Optional.ofNullable(faturasIA).orElseGet(ArrayList::new)
        );

        // 7) Serializa o objeto (não use toString())
        String resultadoJson;
        try {
            resultadoJson = mapper.writeValueAsString(fResponse);
        } catch (JsonProcessingException e) {
          System.out.println("Erro serializando FaturaResponseIA "+e);

            // fallback mínimo para não quebrar o fluxo
            resultadoJson = "{\"faturas\":[]}";
        }

        // 8) Retorna já no formato de mensagem de sistema
        return new ChatMessageDTO("system", "Dado do sistema: " + resultadoJson);
    }
}
