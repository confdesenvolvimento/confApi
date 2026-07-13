package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.confApi.chatconfianca.dto.enums.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class AtendenteStatus {
    private Integer codgUsuario;
    private Long departamentoUnidadeId;
    private StatusAtendente status;
    private Integer atendimentosAtivos;
    private LocalDateTime ultimaAtividadeEm;
    private LocalDateTime atualizadoEm;
}