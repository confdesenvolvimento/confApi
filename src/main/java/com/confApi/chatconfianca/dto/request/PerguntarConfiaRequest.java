package com.confApi.chatconfianca.dto.request;

import com.confApi.chatconfianca.contexto.ChatContextoClienteDTO;
import com.confApi.chatconfianca.dto.enums.PrioridadeConversa;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class PerguntarConfiaRequest {
    private Long conversaId;
    private Integer codgUsuario;
    private Integer codgAgenciaSessao;
    private Long departamentoUnidadeId;
    private String assunto;
    private String mensagem;
    private PrioridadeConversa prioridade;
    private Boolean encaminharAtendente;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ChatContextoClienteDTO contextoCliente;
}
