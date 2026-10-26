package com.confApi.chatgpt.dto;

public record ChatActionDTO(
        String code,
        String label,
        String description,
        String localizador,
        Integer reservaId,
        Boolean requiresConfirmation,
        Boolean requiresRules,
        Boolean sensitive,
        String prompt) {

    public ChatActionDTO(String code,
                         String label,
                         String description,
                         String localizador,
                         Boolean requiresConfirmation,
                         Boolean requiresRules,
                         Boolean sensitive,
                         String prompt) {
        this(code, label, description, localizador, null,
                requiresConfirmation, requiresRules, sensitive, prompt);
    }
}
