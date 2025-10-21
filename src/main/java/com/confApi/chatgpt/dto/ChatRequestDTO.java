package com.confApi.chatgpt.dto;

import com.confApi.chatgpt.tools.ToolDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public record ChatRequestDTO(
        @NotNull List<ChatMessageDTO> messages,
        String model,
        boolean stream,
        List<ToolDefinition> tools,
        Map<String,Object> metadata
) {}
