package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MensagensNaoLidasResumo {
    private long conversasNaoLidas;
    private long mensagensNaoLidas;
    private Long conversaDestaqueId;
}
