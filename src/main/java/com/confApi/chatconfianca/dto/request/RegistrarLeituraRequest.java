package com.confApi.chatconfianca.dto.request;

import lombok.Data;

@Data
public class RegistrarLeituraRequest {
    private Long conversaId;
    private Integer codgUsuario;
    private Boolean gestor;
}