package com.confApi.chatgpt.controller;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.confApi.chatgpt.service.*;
import com.confApi.chatgpt.dto.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Flux;

import javax.validation.Valid;

// ChatController.java
@RestController
@RequestMapping("/v1/ai")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService service;

    @PostMapping("/chat")
    public ChatResponseDTO chat(@Valid @RequestBody ChatRequestDTO req) throws IOException {
        return service.chat(req);
    }

    @PostMapping(value="/chat/stream", produces= MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequestDTO req) {
        return service.stream(req);
    }
}
