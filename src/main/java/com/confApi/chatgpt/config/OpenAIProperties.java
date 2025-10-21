package com.confApi.chatgpt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "confianca.openai")
@Data
public class OpenAIProperties {
    private String apiKey ="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";
    private String baseUrl="https://api.openai.com";
    private String chatModel="gpt-4o";
    private String sttModel="gpt-4o-transcribe";
    private String ttsModel="gpt-4o-mini-tts";
    private String realtimeModel="gpt-realtime";
    private String organization="org-mW61vyv7JdK9NBJN8lm8F69s";
    private String project= "proj_KyvN7N3B2a9jQiKXekOhUx9Q";
}