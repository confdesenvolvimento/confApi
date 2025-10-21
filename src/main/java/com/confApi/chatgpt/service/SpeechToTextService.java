package com.confApi.chatgpt.service;

import com.confApi.chatgpt.config.OpenAIProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

// SpeechToTextService.java
@Service
@RequiredArgsConstructor
public class SpeechToTextService {
    private final OkHttpClient client;
    private final OpenAIProperties props;

    public String transcribe(MultipartFile audio, String model, String language) throws IOException {
        String useModel = (model==null || model.isBlank()) ? props.getSttModel() : model;

        MultipartBody.Builder mb = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("model", useModel)
                .addFormDataPart("file", audio.getOriginalFilename(),
                        RequestBody.create(MediaType.parse(audio.getContentType()), audio.getBytes()));
        if (language != null && !language.isBlank()) mb.addFormDataPart("language", language);

        Request req = new Request.Builder()
                .url(props.getBaseUrl() + "/v1/audio/transcriptions")
                .post(mb.build())
                .build();
        try (Response r = client.newCall(req).execute()) {
            String json = Objects.requireNonNull(r.body()).string();
            return new ObjectMapper().readTree(json).path("text").asText();
        }
    }
}

