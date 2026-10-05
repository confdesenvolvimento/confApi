package com.confApi.chatgpt.service;

import com.confApi.chatgpt.config.OpenAIProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;

/** Per-turn Responses transport. Public chat DTOs and local tool execution remain unchanged. */
public final class OpenAIResponsesAdapter {
    private final ObjectMapper mapper;
    private final List<Object> input = new ArrayList<>();
    private final Set<String> allowedTools = new HashSet<>();
    private final Set<String> seenCalls = new HashSet<>();
    private final Set<String> pendingCalls = new HashSet<>();
    private int consumedMessages;
    private int requests;

    public OpenAIResponsesAdapter(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper);
    }

    public static boolean usesResponses(String model) {
        return "gpt-6.1-sol".equals(model)
                || model != null && model.startsWith("gpt-6.1-sol-");
    }

    public static String model(String requested, OpenAIProperties properties) {
        return requested == null || requested.isBlank() ? properties.getChatModel() : requested.trim();
    }

    private static String effort(OpenAIProperties properties) throws IOException {
        String value = properties.getChatReasoningEffort();
        value = value == null || value.isBlank() ? "low" : value.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("low", "medium", "high", "xhigh", "max").contains(value))
            throw new IOException("OPENAI_REASONING_EFFORT_INVALIDO");
        return value;
    }

    private static int budget(OpenAIProperties properties) throws IOException {
        int value = properties.getChatMaxOutputTokens();
        if (value == 0) value = 8192;
        if (value < 256 || value > 128000) throw new IOException("OPENAI_OUTPUT_BUDGET_INVALIDO");
        return value;
    }

    /** GPT-6.1 text/JSON/stream calls may keep Chat Completions when no tools are sent. */
    public static void configureTextCompletion(Map<String,Object> payload, OpenAIProperties properties,
                                               int maximum) throws IOException {
        if (!usesResponses(Objects.toString(payload.get("model"), null))) return;
        payload.put("reasoning_effort", effort(properties));
        payload.put("max_completion_tokens", Math.min(maximum, budget(properties)));
        payload.put("store", false);
    }

    public Map<String,Object> request(Map<String,Object> chat, OpenAIProperties properties) throws IOException {
        if (++requests > 8) throw new IOException("OPENAI_TOOL_ROUNDS_EXCEDIDOS");
        Object messagesValue = chat.get("messages");
        if (!(messagesValue instanceof List<?> messages) || consumedMessages > messages.size())
            throw new IOException("OPENAI_HISTORICO_INVALIDO");
        for (int i = consumedMessages; i < messages.size(); i++) {
            if (!(messages.get(i) instanceof Map<?,?> message)) throw new IOException("OPENAI_MENSAGEM_INVALIDA");
            String role = Objects.toString(message.get("role"), "");
            if ("assistant".equals(role) && message.containsKey("tool_calls")) {
                // Already replayed below from the original response, including its reasoning items.
                continue;
            }
            if ("tool".equals(role)) {
                String callId = Objects.toString(message.get("tool_call_id"), "");
                if (!pendingCalls.remove(callId)) throw new IOException("OPENAI_TOOL_OUTPUT_SEM_CHAMADA");
                input.add(Map.of("type", "function_call_output", "call_id", callId,
                        "output", Objects.toString(message.get("content"), "")));
            } else {
                if (!Set.of("system", "developer", "user", "assistant").contains(role))
                    throw new IOException("OPENAI_ROLE_INVALIDO");
                input.add(Map.of("role", role, "content", Objects.toString(message.get("content"), "")));
            }
        }
        consumedMessages = messages.size();
        if (!pendingCalls.isEmpty()) throw new IOException("OPENAI_TOOL_OUTPUT_AUSENTE");
        Map<String,Object> payload = new LinkedHashMap<>();
        payload.put("model", chat.get("model"));
        payload.put("input", new ArrayList<>(input));
        payload.put("reasoning", Map.of("effort", effort(properties)));
        payload.put("max_output_tokens", chat.containsKey("max_completion_tokens")
                ? Math.min(4096, budget(properties)) : budget(properties));
        payload.put("store", false);
        payload.put("include", List.of("reasoning.encrypted_content"));
        allowedTools.clear();
        if (chat.get("tools") instanceof List<?> tools) {
            List<Map<String,Object>> definitions = new ArrayList<>();
            for (Object tool : tools) {
                if (!(tool instanceof Map<?,?> definition)
                        || !(definition.get("function") instanceof Map<?,?> function))
                    throw new IOException("OPENAI_TOOL_SCHEMA_INVALIDO");
                String name = Objects.toString(function.get("name"), "");
                if (name.isBlank() || !allowedTools.add(name)) throw new IOException("OPENAI_TOOL_NOME_INVALIDO");
                definitions.add(Map.of("type", "function", "name", name,
                        "description", Objects.toString(function.get("description"), ""),
                        "parameters", function.get("parameters"), "strict", false));
            }
            payload.put("tools", definitions);
            // Retain optional fields in the existing schemas and execute one local tool at a time.
            payload.put("parallel_tool_calls", false);
        }
        Object choice = chat.get("tool_choice");
        if (choice instanceof Map<?,?> choiceMap && choiceMap.get("function") instanceof Map<?,?> function)
            payload.put("tool_choice", Map.of("type", "function", "name", function.get("name")));
        else if (choice != null) payload.put("tool_choice", choice);
        return payload;
    }

    /** Normalize only at the transport boundary; never return reasoning to the client or chat history. */
    public JsonNode completion(JsonNode response) throws IOException {
        if (response == null || !response.isObject()
                || !"completed".equals(response.path("status").asText()) || response.hasNonNull("error"))
            throw new IOException("OPENAI_RESPOSTA_INCOMPLETA");
        JsonNode output = response.path("output");
        if (!output.isArray()) throw new IOException("OPENAI_OUTPUT_INVALIDO");
        StringBuilder text = new StringBuilder();
        List<Map<String,Object>> calls = new ArrayList<>();
        Set<String> newCalls = new HashSet<>();
        for (JsonNode item : output) {
            switch (item.path("type").asText()) {
                case "reasoning" -> { /* Kept for this turn only in the original output items. */ }
                case "message" -> {
                    if (!"assistant".equals(item.path("role").asText())
                            || !"completed".equals(item.path("status").asText()) || !item.path("content").isArray())
                        throw new IOException("OPENAI_MENSAGEM_INCOMPLETA");
                    for (JsonNode part : item.path("content")) {
                        if (!"output_text".equals(part.path("type").asText()) || !part.path("text").isTextual())
                            throw new IOException("OPENAI_RESPOSTA_NAO_TEXTUAL");
                        text.append(part.path("text").asText());
                    }
                }
                case "function_call" -> {
                    String name = item.path("name").asText();
                    String callId = item.path("call_id").asText();
                    if (!allowedTools.contains(name) || callId.isBlank() || seenCalls.contains(callId) || !newCalls.add(callId)
                            || item.hasNonNull("status") && !"completed".equals(item.path("status").asText())
                            || !item.path("arguments").isTextual())
                        throw new IOException("OPENAI_FERRAMENTA_NAO_AUTORIZADA");
                    String arguments = item.path("arguments").asText();
                    JsonNode params = mapper.readTree(arguments);
                    if (params == null || !params.isObject()) throw new IOException("OPENAI_TOOL_ARGUMENTOS_INVALIDOS");
                    calls.add(Map.of("id", callId, "type", "function",
                            "function", Map.of("name", name, "arguments", arguments)));
                }
                default -> throw new IOException("OPENAI_OUTPUT_NAO_SUPORTADO");
            }
        }
        if (calls.isEmpty() && text.toString().isBlank()) throw new IOException("OPENAI_RESPOSTA_VAZIA");
        // Replay every typed output item in order (especially reasoning) before function_call_output.
        input.addAll(mapper.convertValue(output, new TypeReference<List<Map<String,Object>>>() { }));
        seenCalls.addAll(newCalls);
        pendingCalls.addAll(newCalls);
        Map<String,Object> message = new LinkedHashMap<>();
        message.put("content", text.toString());
        if (!calls.isEmpty()) message.put("tool_calls", calls);
        return mapper.valueToTree(Map.of("id", response.path("id").asText(),
                "choices", List.of(Map.of("finish_reason", calls.isEmpty() ? "stop" : "tool_calls", "message", message))));
    }
}
