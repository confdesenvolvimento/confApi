package com.confApi.notificacao;

import lombok.Data;

import java.util.Date;

@Data
public class Notificacao {
    private Integer codgNotificacao;
    private Integer tipoNotificacao;
    private String descricaoCorpo;
    private String descricaoTitulo;
    private String descricaoSubtitulo;
    private Date dataCriacao = new Date();
    private Integer status;
    private Date validade = new Date();
    private String link;
    private Integer nivelVisualizacao;

    public Notificacao() {
    }

    public Notificacao(Integer codgNotificacao, Integer tipoNotificacao, String descricaoCorpo,
                       String descricaoTitulo, String descricaoSubtitulo, Date dataCriacao, Integer status,
                       Date validade, String link, Integer nivelVisualizacao) {
        this.codgNotificacao = codgNotificacao;
        this.tipoNotificacao = tipoNotificacao;
        this.descricaoCorpo = descricaoCorpo;
        this.descricaoTitulo = descricaoTitulo;
        this.descricaoSubtitulo = descricaoSubtitulo;
        this.dataCriacao = dataCriacao;
        this.status = status;
        this.validade = validade;
        this.link = link;
        this.nivelVisualizacao = nivelVisualizacao;
    }

    public Notificacao(Integer tipoNotificacao, String descricaoCorpo, String descricaoTitulo, String descricaoSubtitulo) {
        this.tipoNotificacao = tipoNotificacao;
        this.descricaoCorpo = descricaoCorpo;
        this.descricaoTitulo = descricaoTitulo;
        this.descricaoSubtitulo = descricaoSubtitulo;
        this.dataCriacao = new Date();
        this.status = 0;
        this.validade = new Date();
        this.link = "";
        this.nivelVisualizacao = 0;
    }
}
