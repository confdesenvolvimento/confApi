package com.confApi.chatconfianca.intencao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatIaEncaminhamentoAuditRequest {
    private Long conversaId;
    private Long departamentoAtendimentoId;
}
