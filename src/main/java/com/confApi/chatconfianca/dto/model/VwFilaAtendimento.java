package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.confApi.chatconfianca.dto.enums.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class VwFilaAtendimento {
    private Long id;
    private Long conversaId;
    private String protocolo;
    private String assunto;
    private StatusFila status;
    private PrioridadeConversa prioridade;
    private Integer posicao;
    private LocalDateTime entrouEm;
    private String departamentoNome;
    private String nomeUnidade;
    private String nomeAgencia;
    private String solicitanteNome;
    private String atendenteDestinoNome;
}