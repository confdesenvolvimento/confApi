package com.confApi.hub.aereo;

import com.confApi.endPoints.bilhete.BilheteResponse;

import java.io.Serializable;
import java.util.Date;

public class BilheteModel implements Serializable {

    private Integer status;
    private String numeroBilhete;
    private Date dataEmissao;
    private Date dataCancelamento;
    private Boolean isCancelar = false;

    public BilheteModel(BilheteResponse bilheteResponse) {
        this.status = bilheteResponse.getStatus();
        this.numeroBilhete = bilheteResponse.getNumeroBilhete();
        this.dataEmissao = bilheteResponse.getDataEmissao();
        this.dataCancelamento = bilheteResponse.getDataCancelamento();
        this.isCancelar = bilheteResponse.getIsCancelar();
    }

    public BilheteModel() {
    }


    public BilheteModel(Integer status, String numeroBilhete, Date dataEmissao, Date dataCancelamento) {
        this.status = status;
        this.numeroBilhete = numeroBilhete;
        this.dataEmissao = dataEmissao;
        this.dataCancelamento = dataCancelamento;
    }

    public Boolean getIsCancelar() {
        return isCancelar;
    }

    public void setIsCancelar(Boolean isCancelar) {
        this.isCancelar = isCancelar;
    }



    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getNumeroBilhete() {
        return numeroBilhete;
    }

    public void setNumeroBilhete(String numeroBilhete) {
        this.numeroBilhete = numeroBilhete;
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

