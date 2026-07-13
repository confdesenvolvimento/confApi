package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ParametroSistema {
    private String chave;
    private String valor;
    private String descricao;
    private LocalDateTime atualizadoEm;
}