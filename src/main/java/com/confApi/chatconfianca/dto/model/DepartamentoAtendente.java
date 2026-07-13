package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.confApi.chatconfianca.dto.enums.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class DepartamentoAtendente {
    private Long id;
    private Long departamentoUnidadeId;
    private Integer codgUsuario;
    private PapelAtendente papel;
    private Boolean recebeChamados;
    private Integer prioridadeDistribuicao;
    private Integer limiteChatsSimultaneos;
    private Boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}