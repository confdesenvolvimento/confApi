package com.confApi.notificacao;

import com.confApi.db.confManager.usuario.Usuario;
import lombok.Data;

import java.util.Date;

@Data
public class NotificacaoControle {
    private Integer codgNotificacaoControle;
    private Integer lido;
    private Date dataLeitura;
    private Usuario usuario;
    private NotificacaoConfig notificacaoConfig;
}
