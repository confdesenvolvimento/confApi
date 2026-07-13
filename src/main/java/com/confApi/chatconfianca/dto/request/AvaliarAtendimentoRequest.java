package com.confApi.chatconfianca.dto.request;

import lombok.Data;

@Data
public class AvaliarAtendimentoRequest {
    private Long conversaId;
    private Integer codgUsuarioAvaliador;
    private Integer nota;
    private String comentario;
}