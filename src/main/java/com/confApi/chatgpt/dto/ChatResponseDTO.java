package com.confApi.chatgpt.dto;

import java.util.List;

public record ChatResponseDTO(
        String id,
        String content,
        List<ToolCallDTO> toolCalls,
        ChatAudioDTO audio,
        List<String> keywords,
        List<ChatMessageDTO> history,
        List<ChatActionDTO> actions) {

    public ChatResponseDTO(String id,
                           String content,
                           List<ToolCallDTO> toolCalls,
                           ChatAudioDTO audio,
                           List<String> keywords,
                           List<ChatMessageDTO> history) {
        this(id, content, toolCalls, audio, keywords, history, List.of());
    }
}
