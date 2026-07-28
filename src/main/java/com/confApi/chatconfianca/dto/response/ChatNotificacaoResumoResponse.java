package com.confApi.chatconfianca.dto.response;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatNotificacaoResumoResponse {
    private Integer codgUsuario;
    private boolean atendente;
    private boolean gestor;
    private boolean admin;
    private long filasAguardando;
    private long conversasAtendenteNaoLidas;
    private long mensagensAtendenteNaoLidas;
    private Long conversaAtendenteDestaqueId;
    private long conversasUsuarioNaoLidas;
    private long mensagensUsuarioNaoLidas;
    private Long conversaUsuarioDestaqueId;
    private long totalPendencias;
    private LocalDateTime atualizadoEm;
}
