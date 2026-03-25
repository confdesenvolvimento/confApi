package com.confApi.chatgpt.dto;

import com.confApi.chatgpt.tools.EnumTipoAssistenciaCampo;
import org.jetbrains.annotations.NotNull;

import javax.validation.constraints.NotBlank;
import java.util.Map;

public record FieldAssistantRequestDTO(
        @NotNull EnumTipoAssistenciaCampo tipo,
        @NotBlank String campo,
        String labelCampo,
        String valorAtual,
        String contexto,
        Integer codgAgencia,
        Integer codgUsuario,
        String tom,
        String tamanho,
        Map<String, String> dadosExtras
) {
}
