package com.confApi.chatgpt.dto;

import java.util.List;

public record FieldAssistantResponseDTO(
        String campo,
        String tipo,
        String original,
        String resultado,
        String resumo,
        List<String> sugestoes,
        String observacao
) {
}