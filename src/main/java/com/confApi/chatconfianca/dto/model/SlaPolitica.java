package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.confApi.chatconfianca.dto.enums.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class SlaPolitica {
    private Long id;
    private Long departamentoUnidadeId;
    private PrioridadeConversa prioridade;
    private Integer primeiraRespostaMinutos;
    private Integer resolucaoMinutos;
    private Integer alertaAntesMinutos;
    private Boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}