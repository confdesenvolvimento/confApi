package com.confApi.chatconfianca.dto.request;

import lombok.Data;

@Data
public class AdicionarTagConversaRequest {
    private Long conversaId;
    private Long tagId;
    private String nome;
    private String corHex;
    private Integer codgUsuario;
    private Boolean gestor;
}
