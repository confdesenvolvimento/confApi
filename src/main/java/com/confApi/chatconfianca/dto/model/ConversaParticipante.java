package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.confApi.chatconfianca.dto.enums.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ConversaParticipante {
    private Long id;
    private Long conversaId;
    private Integer codgUsuario;
    private PapelParticipante papel;
    private Boolean ativo = true;
    private LocalDateTime entrouEm;
    private LocalDateTime saiuEm;
    private LocalDateTime ultimaVisualizacaoEm;
    private Boolean silenciado = false;
}