package com.confApi.chatgpt.dto;

import java.util.List;

public record ChatResponseDTO(String id, String content, List<ToolCallDTO> toolCalls) {}