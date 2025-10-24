// TextToSpeechService.java
package com.confApi.chatgpt.service;

import com.confApi.chatgpt.config.OpenAIProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TextToSpeechService {
    private final OkHttpClient baseClient;
    private final OpenAIProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    public byte[] synthesize(String text, String model, String voice, String fmt) throws IOException {
        String useModel = (model == null || model.isBlank()) ? "tts-1" : model;  // ou "gpt-4o-mini-tts"
        String useVoice = (voice == null || voice.isBlank()) ? "echo" : voice;
        String useFmt = (fmt == null || fmt.isBlank()) ? "mp3" : fmt.toLowerCase();

        String json = mapper.writeValueAsString(Map.of(
                "model", useModel,
                "input", text,
                "voice", useVoice,
                "format", useFmt
        ));

        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/audio/speech"))
                .header("Authorization", "Bearer " + props.getApiKey().trim())
                .header("Content-Type", "application/json")
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "curl/8.7.1")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        try {
            HttpResponse<byte[]> r = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (r.statusCode() != 200) {
                String err = new String(r.body());
                throw new IOException("TTS falhou: HTTP " + r.statusCode() + " - " + err);
            }
            byte[] audio = r.body();
            Path out = Files.createTempFile("tts-", "." + useFmt);
            Files.write(out, audio);
            System.out.println("Áudio salvo em: " + out.toAbsolutePath() + " (" + audio.length + " bytes)");
            return audio;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("TTS interrompido", ie);
        }
    }

}
