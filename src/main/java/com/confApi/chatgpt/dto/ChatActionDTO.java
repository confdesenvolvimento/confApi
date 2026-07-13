package com.confApi.chatgpt.dto;

public record ChatActionDTO(
        String code,
        String label,
        String description,
        String localizador,
        Boolean requiresConfirmation,
        Boolean requiresRules,
        Boolean sensitive,
        String prompt) {
}
