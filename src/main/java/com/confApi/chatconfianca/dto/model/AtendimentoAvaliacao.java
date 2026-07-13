package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class AtendimentoAvaliacao {
    private Long id;
    private Long conversaId;
    private Integer codgUsuarioAvaliador;
    private Integer nota;
    private String comentario;
    private LocalDateTime criadoEm;
}