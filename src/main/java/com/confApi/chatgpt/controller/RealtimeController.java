package com.confApi.chatgpt.controller;
import com.confApi.chatgpt.config.OpenAIProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// RealtimeController.java (expondo apenas as "capabilities" e modelo)
@RestController
@RequestMapping("/v1/ai/realtime")
@RequiredArgsConstructor
public class RealtimeController {
    private final OpenAIProperties props;

    @GetMapping("/capabilities")
    public Map<String,Object> capabilities() {
        return Map.of(
                "model", props.getRealtimeModel(),
                "transport", "websocket",
                "endpoint", "wss://api.openai.com/v1/realtime", // cliente se conecta direto
                "features", List.of("audio_in","audio_out","function_calling")
        );
    }
}
