package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ConversaTag {
    private Long conversaId;
    private Long tagId;
    private Integer criadoPorCodgUsuario;
    private LocalDateTime criadoEm;
}
