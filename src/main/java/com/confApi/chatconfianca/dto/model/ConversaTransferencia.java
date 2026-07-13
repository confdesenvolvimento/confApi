package com.confApi.chatconfianca.dto.model;

import com.confApi.chatconfianca.dto.enums.StatusTransferencia;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ConversaTransferencia {
    private Long id;
    private Long conversaId;
    private Long departamentoUnidadeOrigemId;
    private Long departamentoUnidadeDestinoId;
    private Integer atendenteOrigemCodgUsuario;
    private Integer atendenteDestinoCodgUsuario;
    private String motivo;
    private StatusTransferencia status;
    private LocalDateTime solicitadoEm;
    private LocalDateTime respondidoEm;
}
