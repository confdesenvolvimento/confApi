package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class MensagemLeitura {
    private Long mensagemId;
    private Integer codgUsuario;
    private LocalDateTime entregueEm;
    private LocalDateTime lidaEm;
}