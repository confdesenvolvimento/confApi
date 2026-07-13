package com.confApi.chatconfianca.dto.request;

import lombok.Data;

@Data
public class EnviarMensagemRequest {
    private Long conversaId;
    private Integer codgUsuario;
    private String conteudo;
    private Boolean interna;
    private Boolean gestor;
}