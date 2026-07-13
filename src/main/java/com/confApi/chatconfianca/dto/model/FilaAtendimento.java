package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.confApi.chatconfianca.dto.enums.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class FilaAtendimento {
    private Long id;
    private Long conversaId;
    private Long departamentoUnidadeId;
    private Integer codgUnidade;
    private Integer codgAgencia;
    private Integer solicitanteCodgUsuario;
    private StatusFila status;
    private PrioridadeConversa prioridade;
    private Integer posicao;
    private Integer atendenteDestinoCodgUsuario;
    private LocalDateTime entrouEm;
    private LocalDateTime chamadoEm;
    private LocalDateTime saiuEm;
    private String motivoSaida;
}