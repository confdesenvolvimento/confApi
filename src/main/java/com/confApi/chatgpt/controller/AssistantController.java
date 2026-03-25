package com.confApi.chatgpt.controller;

import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.service.ChatService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/ia/assistant")
@RequiredArgsConstructor
public class AssistantController {
    private final ChatService chatService;

    @PostMapping(value = "/field", produces = MediaType.APPLICATION_JSON_VALUE)
    public FieldAssistantResponseDTO assistField(@Valid @RequestBody FieldAssistantRequestDTO req) throws IOException {
        return chatService.assistField(req);
    }


}
