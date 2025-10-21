package com.confApi.chatgpt.service;

import com.confApi.chatgpt.config.OpenAIProperties;
import com.confApi.chatgpt.dto.ChatRequestDTO;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.confApi.chatgpt.dto.ToolCallDTO;
import com.confApi.chatgpt.tools.ToolRouter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import okio.BufferedSource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.*;

// ChatService.java
@Service
@RequiredArgsConstructor
public class ChatService {
    private final OkHttpClient client;
    private final OpenAIProperties props;
    private final ToolRouter tools;

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

            return new ChatResponseDTO(root.path("id").asText(), content, toolCalls);
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
}
