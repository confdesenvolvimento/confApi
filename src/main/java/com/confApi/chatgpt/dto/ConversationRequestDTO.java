package com.confApi.chatgpt.dto;

import org.jetbrains.annotations.NotNull;

import javax.validation.constraints.NotBlank;
import java.util.List;

public record ConversationRequestDTO(
        @NotBlank String identificacao,
        @NotBlank String unidade,// ex: "confia-voos", "confia-hoteis"
        @NotNull Long codgAgencia,
        @NotNull Long codgUsuario,
        @NotBlank String input,             // mensagem do usuário
        List<ChatMessageDTO> history,       // opcional: histórico já tokenizado (role/content)
        String model,                       // opcional: sobrepor modelo
        Boolean stream                      // opcional: sobrepor streaming no endpoint /stream
) {}
