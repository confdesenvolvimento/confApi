package com.confApi.chatgpt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Response;
import java.io.IOException;

final class OpenAiCompletionResponse {
    static JsonNode read(Response response, ObjectMapper mapper) throws IOException {
        if (!response.isSuccessful()) throw new OpenAiUnavailableException("OPENAI_HTTP_" + response.code());
        if (response.body() == null) throw new OpenAiUnavailableException("OPENAI_EMPTY_RESPONSE");
        JsonNode root;
        try { root = mapper.readTree(response.body().string()); }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new OpenAiUnavailableException("OPENAI_INVALID_JSON");
        }
        if (root == null || !root.isObject() || root.hasNonNull("error"))
            throw new OpenAiUnavailableException("OPENAI_INVALID_RESPONSE");
        return root;
    }
    static JsonNode firstChoice(JsonNode root) throws OpenAiUnavailableException {
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty() || !choices.get(0).path("message").isObject())
            throw new OpenAiUnavailableException("OPENAI_MISSING_COMPLETION");
        return choices.get(0);
    }
}
