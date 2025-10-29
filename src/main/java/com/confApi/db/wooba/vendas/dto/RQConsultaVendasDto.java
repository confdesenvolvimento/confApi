package com.confApi.db.wooba.vendas.dto;

import java.io.Serializable;

public class RQConsultaVendasDto implements Serializable {

    private String localizador = null;
    private String passageiro = null;
    private String numeroBilhete = null;
    private Integer usuario = null;
    private Boolean isUltimasVendas = false;

    public RQConsultaVendasDto() {
    }

    public RQConsultaVendasDto(String localizador, String passageiro, String numeroBilhete, Integer usuario) {
        this.localizador = localizador;
        this.passageiro = passageiro;
        this.numeroBilhete = numeroBilhete;
        this.usuario = usuario;
    }

    public String getLocalizador() {
        return localizador;
    }

    public void setLocalizador(String localizador) {
        this.localizador = localizador;
    }

    public String getPassageiro() {
        return passageiro;
    }

    public void setPassageiro(String passageiro) {
        this.passageiro = passageiro;
    }

    public String getNumeroBilhete() {
        return numeroBilhete;
    }

    public void setNumeroBilhete(String numeroBilhete) {
        this.numeroBilhete = numeroBilhete;
    }

    public Integer getUsuario() {
        return usuario;
    }

    public void setUsuario(Integer usuario) {
        this.usuario = usuario;
    }

    public Boolean getIsUltimasVendas() {
        return isUltimasVendas;
    }

    public void setIsUltimasVendas(Boolean isUltimasVendas) {
        this.isUltimasVendas = isUltimasVendas;
    }

}
