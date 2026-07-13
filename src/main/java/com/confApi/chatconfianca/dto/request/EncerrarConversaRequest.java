package com.confApi.chatconfianca.dto.request;

import lombok.Data;

@Data
public class EncerrarConversaRequest {
    private Long conversaId;
    private Integer codgUsuario;
    private String categoria;
    private String motivo;
    private Boolean gestor;
}