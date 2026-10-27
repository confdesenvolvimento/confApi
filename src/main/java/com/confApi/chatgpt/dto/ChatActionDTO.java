package com.confApi.chatgpt.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ChatActionDTO(
        String code,
        String label,
        String description,
        String localizador,
        Integer reservaId,
        Boolean requiresConfirmation,
        Boolean requiresRules,
        Boolean sensitive,
        String prompt,
        @JsonInclude(JsonInclude.Include.NON_NULL) ChatActionDestinoDTO destino) {

    public ChatActionDTO(String code,
                         String label,
                         String description,
                         String localizador,
                         Integer reservaId,
                         Boolean requiresConfirmation,
                         Boolean requiresRules,
                         Boolean sensitive,
                         String prompt) {
        this(code, label, description, localizador, reservaId,
                requiresConfirmation, requiresRules, sensitive, prompt, null);
    }

    public ChatActionDTO(String code,
                         String label,
                         String description,
                         String localizador,
                         Boolean requiresConfirmation,
                         Boolean requiresRules,
                         Boolean sensitive,
                         String prompt) {
        this(code, label, description, localizador, null,
                requiresConfirmation, requiresRules, sensitive, prompt, null);
    }
}
