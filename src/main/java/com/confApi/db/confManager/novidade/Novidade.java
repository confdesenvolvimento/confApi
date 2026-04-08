package com.confApi.db.confManager.novidade;

import com.confApi.db.confManager.usuario.Usuario;

import javax.persistence.Column;
import java.io.Serializable;
import java.util.Date;

public class Novidade implements Serializable {

    private Integer codgNovidade;
    private String titulo;
    private String descricao;
    private String link;
    private Integer status;
    private Usuario codgUsuario;
    private Date dataCriacao = new Date();
    private String tipo;
    private String categoria;

    public Novidade() {
    }

    public Novidade(Integer codgNovidade, String titulo, String descricao, String link, Integer status,
                    Usuario codgUsuario, Date dataCriacao, String tipo, String categoria) {
        this.codgNovidade = codgNovidade;
        this.titulo = titulo;
        this.descricao = descricao;
        this.link = link;
        this.status = status;
        this.codgUsuario = codgUsuario;
        this.dataCriacao = dataCriacao;
        this.tipo = tipo;
        this.categoria = categoria;
    }

    public Integer getCodgNovidade() {
        return codgNovidade;
    }

    public void setCodgNovidade(Integer codgNovidade) {
        this.codgNovidade = codgNovidade;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Usuario getCodgUsuario() {
        return codgUsuario;
    }

    public void setCodgUsuario(Usuario codgUsuario) {
        this.codgUsuario = codgUsuario;
    }

    public Date getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(Date dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    @Override
    public String toString() {
        return "Novidade{" +
                "codgNovidade=" + codgNovidade +
                ", titulo='" + titulo + '\'' +
                ", descricao='" + descricao + '\'' +
                ", link='" + link + '\'' +
                ", status=" + status +
                ", codgUsuario=" + codgUsuario +
                ", dataCriacao=" + dataCriacao +
                ", tipo='" + tipo + '\'' +
                ", categoria='" + categoria + '\'' +
                '}';
    }
}
