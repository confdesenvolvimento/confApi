package com.confApi.db.confManager.unidade.dto;

import com.confApi.db.wooba.unidade.TurUnidadesOperacionais;

import java.io.Serializable;

public class Unidade implements Serializable {

    private Integer codgUnidade;
    private String nomeUnidade;
    private String codgSistemaBackOffice;
    private Integer status;
    private Integer idWoobaUnidade;

    public Unidade(Integer codgUnidade, Integer idWoobaUnidade) {
        this.codgUnidade = codgUnidade;
        this.idWoobaUnidade = idWoobaUnidade;
    }

    public Unidade(Integer codgUnidade) {
        this.codgUnidade = codgUnidade;
    }

    public Unidade() {
    }

    public Unidade(TurUnidadesOperacionais turUnidade) {
        this.nomeUnidade = turUnidade.getNomeUnidade();
        this.codgSistemaBackOffice = turUnidade.getIderp();
        this.status = turUnidade.getSituacao();
        this.idWoobaUnidade = turUnidade.getId();
    }

    /**
     * @return the codgUnidade
     */
    public Integer getCodgUnidade() {
        return codgUnidade;
    }

    /**
     * @param codgUnidade the codgUnidade to set
     */
    public void setCodgUnidade(Integer codgUnidade) {
        this.codgUnidade = codgUnidade;
    }

    /**
     * @return the nomeUnidade
     */
    public String getNomeUnidade() {
        return nomeUnidade;
    }

    /**
     * @param nomeUnidade the nomeUnidade to set
     */
    public void setNomeUnidade(String nomeUnidade) {
        this.nomeUnidade = nomeUnidade;
    }

    /**
     * @return the codgSistemaBackOffice
     */
    public String getCodgSistemaBackOffice() {
        return codgSistemaBackOffice;
    }

    /**
     * @param codgSistemaBackOffice the codgSistemaBackOffice to set
     */
    public void setCodgSistemaBackOffice(String codgSistemaBackOffice) {
        this.codgSistemaBackOffice = codgSistemaBackOffice;
    }

    /**
     * @return the status
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getIdWoobaUnidade() {
        return idWoobaUnidade;
    }

    public void setIdWoobaUnidade(Integer idWoobaUnidade) {
        this.idWoobaUnidade = idWoobaUnidade;
    }
}