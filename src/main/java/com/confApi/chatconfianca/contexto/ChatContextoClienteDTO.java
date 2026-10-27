package com.confApi.chatconfianca.contexto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** Referencias de interface nao confiaveis; nao representam identidade ou permissao. */
@Data
public class ChatContextoClienteDTO {
    private String schema = "chat.contexto-cliente.v1";
    private String canal;
    private String tela;
    private ChatRecursoReferenciaDTO recurso;
    private List<String> capacidades = new ArrayList<>();
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private Integer copilotoAereoVersao;
}
