package com.confApi.db.confManager.notificacao;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificacaoConfigDTO implements Serializable {

    private Integer codgUnidade;
    private Integer codgAgencia;
    private Integer codgUsuario;
    private String tituloNotificacao;
    private String descricaoNotificacao;
    private String subTituloNotificacao;

    public Integer getCodgUnidade() {
        return codgUnidade;
    }

    public void setCodgUnidade(Integer codgUnidade) {
        this.codgUnidade = codgUnidade;
    }

    public Integer getCodgAgencia() {
        return codgAgencia;
    }

    public void setCodgAgencia(Integer codgAgencia) {
        this.codgAgencia = codgAgencia;
    }

    public Integer getCodgUsuario() {
        return codgUsuario;
    }

    public void setCodgUsuario(Integer codgUsuario) {
        this.codgUsuario = codgUsuario;
    }

    public String getTituloNotificacao() {
        return tituloNotificacao;
    }

    public void setTituloNotificacao(String tituloNotificacao) {
        this.tituloNotificacao = tituloNotificacao;
    }

    public String getDescricaoNotificacao() {
        return descricaoNotificacao;
    }

    public void setDescricaoNotificacao(String descricaoNotificacao) {
        this.descricaoNotificacao = descricaoNotificacao;
    }

    public String getSubTituloNotificacao() {
        return subTituloNotificacao;
    }

    public void setSubTituloNotificacao(String subTituloNotificacao) {
        this.subTituloNotificacao = subTituloNotificacao;
    }
}
