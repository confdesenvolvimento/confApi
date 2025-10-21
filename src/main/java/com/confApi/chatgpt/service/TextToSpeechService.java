package com.confApi.chatgpt.service;

import com.confApi.chatgpt.config.OpenAIProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TextToSpeechService {
    private final OkHttpClient client;
    private final OpenAIProperties props;

    public byte[] synthesize(String text, String model, String voice, String fmt) throws IOException {
        Map<String,Object> body = Map.of(
                "model", (model==null||model.isBlank())? props.getTtsModel():model,
                "input", text,
                "voice", (voice==null||voice.isBlank())? "alloy": voice,
                "format", (fmt==null||fmt.isBlank())? "mp3": fmt
        );
        Request req = new Request.Builder()
                .url(props.getBaseUrl() + "/v1/audio/speech")
                .post(RequestBody.create(
                        MediaType.parse("application/json"),
                        new ObjectMapper().writeValueAsBytes(body)))
                .build();
        try (Response r = client.newCall(req).execute()) {
            return Objects.requireNonNull(r.body()).bytes();
        }
    }
}