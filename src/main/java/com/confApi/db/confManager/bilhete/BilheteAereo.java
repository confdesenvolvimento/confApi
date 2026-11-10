package com.confApi.db.confManager.bilhete;

import com.confApi.db.confManager.passageiro.Passageiro;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.hub.aereo.BilheteModel;
import com.confApi.hub.aereo.PassageiroModel;
import com.confApi.hub.aereo.ReservaAereoModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BilheteAereo implements Serializable {

    private Integer codgBilhete;
    private Passageiro codgPassageiro;
    private Integer status;
    private String numrBilhete;
    private Date dataEmissao;
    private Date dataCancelamento;

    public BilheteAereo() {
    }

    public BilheteAereo(Integer codgBilhete, Passageiro codgPassageiro, Integer status, String numrBilhete, Date dataEmissao, Date dataCancelamento) {
        this.codgBilhete = codgBilhete;
        this.codgPassageiro = codgPassageiro;
        this.status = status;
        this.numrBilhete = numrBilhete;
        this.dataEmissao = dataEmissao;
        this.dataCancelamento = dataCancelamento;
    }

    public BilheteAereo(PassageiroModel passageiro, BilheteModel bilhete) {
        this.codgPassageiro = new Passageiro(passageiro.getCodgPassageiroDb());
        this.status = bilhete.getStatus();
        this.numrBilhete = bilhete.getNumeroBilhete();
        this.dataEmissao = bilhete.getDataEmissao();
        this.dataCancelamento = null;
    }

    public BilheteAereo(Passageiro passageiro, BilheteModel bilhete) {
        this.codgPassageiro = new Passageiro(passageiro.getCodgPassageiro());
        this.status = bilhete.getStatus();
        this.numrBilhete = bilhete.getNumeroBilhete();
        this.dataEmissao = bilhete.getDataEmissao();
        this.dataCancelamento = bilhete.getDataCancelamento();
    }

    public BilheteAereo(BilheteModel bilhete) {
        this.status = bilhete.getStatus();
        this.numrBilhete = bilhete.getNumeroBilhete();
        this.dataEmissao = bilhete.getDataEmissao();
        this.dataCancelamento = bilhete.getDataCancelamento();
    }

    public Integer getCodgBilhete() {
        return codgBilhete;
    }

    public void setCodgBilhete(Integer codgBilhete) {
        this.codgBilhete = codgBilhete;
    }

    public Passageiro getCodgPassageiro() {
        return codgPassageiro;
    }

    public void setCodgPassageiro(Passageiro codgPassageiro) {
        this.codgPassageiro = codgPassageiro;
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

    @Override
    public String toString() {
        return "BilheteAereo{" +
                "codgBilhete=" + codgBilhete +
                ", codgPassageiro=" + codgPassageiro +
                ", status=" + status +
                ", numrBilhete='" + numrBilhete + '\'' +
                ", dataEmissao=" + dataEmissao +
                ", dataCancelamento=" + dataCancelamento +
                '}';
    }
}
