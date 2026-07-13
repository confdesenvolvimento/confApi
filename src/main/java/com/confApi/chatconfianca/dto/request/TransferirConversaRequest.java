package com.confApi.chatconfianca.dto.request;

import lombok.Data;

@Data
public class TransferirConversaRequest {
    private Long conversaId;
    private Integer codgUsuario;
    private Long departamentoUnidadeDestinoId;
    private Integer codgAtendenteDestino;
    private String motivo;
    private Boolean gestor;
}
