package com.confApi.db.confManager.sistema;

import com.confApi.db.confManager.produto.Produto;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Sistema implements Serializable {

    private Integer codgSistema;
    private String nomeSistema;
    private Integer status;
    private Integer flagTipoTrecho;
    private String endpointProd;
    private String logoSmall;
    private String logo;
    private String siglaSistema;
    private Produto produto;

    public Sistema(Integer codgSistema) {
        this.codgSistema = codgSistema;
    }



    public Sistema(String nomeSistema, String siglaSistema) {
        this.nomeSistema = nomeSistema;
        this.siglaSistema = siglaSistema;
    }

    public Sistema() {
    }




    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public Integer getCodgSistema() {
        return codgSistema;
    }

    public void setCodgSistema(Integer codgSistema) {
        this.codgSistema = codgSistema;
    }

    public String getNomeSistema() {
        return nomeSistema;
    }

    public void setNomeSistema(String nomeSistema) {
        this.nomeSistema = nomeSistema;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getFlagTipoTrecho() {
        return flagTipoTrecho;
    }

    public void setFlagTipoTrecho(Integer flagTipoTrecho) {
        this.flagTipoTrecho = flagTipoTrecho;
    }

    public String getEndpointProd() {
        return endpointProd;
    }

    public void setEndpointProd(String endpointProd) {
        this.endpointProd = endpointProd;
    }

    public String getLogoSmall() {
        return logoSmall;
    }

    public void setLogoSmall(String logoSmall) {
        this.logoSmall = logoSmall;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getSiglaSistema() {
        return siglaSistema;
    }

    public void setSiglaSistema(String siglaSistema) {
        this.siglaSistema = siglaSistema;
    }

}
