package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class RespostaRapida {
    private Long id;
    private Long departamentoId;
    private Integer codgUnidade;
    private String titulo;
    private String texto;
    private String atalho;
    private Boolean ativo;
    private Integer criadoPorCodgUsuario;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}