package com.confApi.notificacao;

import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.unidade.dto.Unidade;
import com.confApi.db.confManager.usuario.Usuario;
import lombok.Data;

import java.util.Date;

@Data
public class NotificacaoConfig {
    private Integer codgNotificacaoConfig;
    private Unidade unidade = new Unidade();
    private Agencia agencia = new Agencia();
    private Usuario usuario = new Usuario();
    private Notificacao notificacao = new Notificacao();

    public NotificacaoConfig() {
    }

    public NotificacaoConfig(Integer codgNotificacaoConfig, Unidade unidade, Agencia agencia, Usuario usuario, Notificacao notificacao) {
        this.codgNotificacaoConfig = codgNotificacaoConfig;
        this.unidade = unidade;
        this.agencia = agencia;
        this.usuario = usuario;
        this.notificacao = notificacao;
    }

    public NotificacaoConfig(String corpo, String titulo, String subtitulo, Integer tipoNotificacao, Usuario usuario) {
        this.unidade = usuario.getUnidade();
        this.agencia = usuario.getAgencia();
        this.usuario = usuario;
        this.notificacao = new Notificacao(tipoNotificacao, corpo, titulo, subtitulo);
    }
}
