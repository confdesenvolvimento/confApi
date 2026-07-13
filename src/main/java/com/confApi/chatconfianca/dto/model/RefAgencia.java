package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class RefAgencia {
    private Integer codgAgencia;
    private Integer codgUnidade;
    private String nomeAgencia;
    private String cnpj;
    private String codgSistemaBackoffice;
    private Integer status;
    private Integer idWoobaAgencia;
    private Boolean ativoChat;
    private LocalDateTime sincronizadoEm;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}