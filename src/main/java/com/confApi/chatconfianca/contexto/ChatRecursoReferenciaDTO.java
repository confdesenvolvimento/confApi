package com.confApi.chatconfianca.contexto;

import lombok.Data;

/** Chave de recurso. Titularidade deve ser verificada na origem em cada operacao. */
@Data
public class ChatRecursoReferenciaDTO {
    private String tipo;
    private String id;
    private String produto;
    private String dataEmissao;
}
