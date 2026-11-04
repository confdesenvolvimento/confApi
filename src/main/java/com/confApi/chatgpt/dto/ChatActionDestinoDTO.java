package com.confApi.chatgpt.dto;

import com.confApi.chatconfianca.contexto.ChatRecursoReferenciaDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/** Destino sem URL livre, interpretado por adaptadores explicitamente suportados pelo cliente. */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatActionDestinoDTO {
    private String tela;
    private ChatRecursoReferenciaDTO recurso;
    private String documento;
}
