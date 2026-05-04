package com.confApi.db.confManager.bannerApp;

import com.confApi.db.confManager.usuario.Usuario;

import java.io.Serializable;
import java.util.Date;

public class BannerApp implements Serializable {

    private Integer codgBannerApp;
    private String link;
    private Integer status;
    private Usuario codgUsuario;
    private Date dataCriacao = new Date();
    private String categoria;

    public BannerApp() {
    }

    public BannerApp(Integer codgBannerApp, String link, Integer status, Usuario codgUsuario, Date dataCriacao, String categoria) {
        this.codgBannerApp = codgBannerApp;
        this.link = link;
        this.status = status;
        this.codgUsuario = codgUsuario;
        this.dataCriacao = dataCriacao;
        this.categoria = categoria;
    }

    public Integer getCodgBannerApp() {
        return codgBannerApp;
    }

    public void setCodgBannerApp(Integer codgBannerApp) {
        this.codgBannerApp = codgBannerApp;
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

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    @Override
    public String toString() {
        return "BannerApp{" +
                "codgBannerApp=" + codgBannerApp +
                ", link='" + link + '\'' +
                ", status=" + status +
                ", codgUsuario=" + codgUsuario +
                ", dataCriacao=" + dataCriacao +
                ", categoria='" + categoria + '\'' +
                '}';
    }
}
