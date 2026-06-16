package com.confApi.db.confManager.bilhete;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BilheteAereo implements Serializable {

    private Integer codgBilhete;
    private Integer status;
    private String numrBilhete;
    private Date dataEmissao;
    private Date dataCancelamento;

    public Integer getCodgBilhete() {
        return codgBilhete;
    }

    public void setCodgBilhete(Integer codgBilhete) {
        this.codgBilhete = codgBilhete;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getNumrBilhete() {
        return numrBilhete;
    }

    public void setNumrBilhete(String numrBilhete) {
        this.numrBilhete = numrBilhete;
    }

    public Date getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(Date dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public Date getDataCancelamento() {
        return dataCancelamento;
    }

    public void setDataCancelamento(Date dataCancelamento) {
        this.dataCancelamento = dataCancelamento;
    }
}
