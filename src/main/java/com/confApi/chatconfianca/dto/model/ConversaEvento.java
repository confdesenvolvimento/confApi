package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ConversaEvento {
    private Long id;
    private Long conversaId;
    private String tipoEvento;
    private Integer codgUsuario;
    private String descricao;
    private String dadosJson;
    private LocalDateTime criadoEm;
}