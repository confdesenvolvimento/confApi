package com.confApi.chatconfianca.dto.request;

import com.confApi.chatconfianca.dto.enums.PrioridadeConversa;
import lombok.Data;

@Data
public class AbrirConversaRequest {
    private Integer codgUsuario;
    private Integer codgAgenciaSessao;
    private Long departamentoUnidadeId;
    private String assunto;
    private String descricaoInicial;
    private PrioridadeConversa prioridade;
}