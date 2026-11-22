package com.confApi.chatgpt.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

public record ConversationRequestDTO(
        @NotBlank String identificacao,
        @NotBlank String unidade,
        @NotBlank String idErp,// ex: "confia-voos", "confia-hoteis"
        @NotNull Long codgAgencia,
        @NotNull Long codgUsuario,
        @NotBlank String input,             // mensagem do usuário
        List<ChatMessageDTO> history,       // opcional: histórico já tokenizado (role/content)
        String model,                       // opcional: sobrepor modelo
        Boolean stream,
        List<String> keywords

) {}
