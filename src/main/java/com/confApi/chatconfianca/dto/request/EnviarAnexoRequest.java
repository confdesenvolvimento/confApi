package com.confApi.chatconfianca.dto.request;

import lombok.Data;

@Data
public class EnviarAnexoRequest {
    private Long conversaId;
    private Integer codgUsuario;
    private String nomeArquivo;
    private String mimeType;
    private String conteudoBase64;
    private Boolean interna;
    private Boolean gestor;
}