package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class RefUnidade {
    private Integer codgUnidade;
    private String nomeUnidade;
    private String codgSistemaBackoffice;
    private Integer status;
    private Integer idWoobaUnidade;
    private Boolean ativoChat;
    private LocalDateTime sincronizadoEm;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}