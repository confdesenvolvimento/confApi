package com.confApi.db.confManager.companhiaAerea;

import com.confApi.endPoints.companhiaAerea.CompanhiaAereaResponse;
import lombok.Data;

import java.io.Serializable;

@Data
public class CompanhiaAerea implements Serializable {

    private Integer codgCompanhiaAerea;
    private String nomeCia;
    private String iataCia;
    private String numrCia;
    private Integer status;
    private Integer flagReserva;
    private Integer flagEmitir;
    private String codgIntegracao;
    private String imgPesquisa;
    private String imgEmissao;
    private String imhLogoSmall;
    private String imgLogo;

    public CompanhiaAerea(CompanhiaAereaResponse companhiaAereaResponse) {
        this.codgCompanhiaAerea = companhiaAereaResponse.getCodgCompanhiaAerea();
        this.nomeCia = companhiaAereaResponse.getNomeCia();
        this.iataCia = companhiaAereaResponse.getIataCia();
        this.numrCia = companhiaAereaResponse.getNumrCia();
        this.status = companhiaAereaResponse.getStatus();
        this.flagReserva = companhiaAereaResponse.getFlagReserva();
        this.flagEmitir = companhiaAereaResponse.getFlagEmitir();
        this.codgIntegracao = companhiaAereaResponse.getCodgIntegracao();
        this.imgPesquisa = companhiaAereaResponse.getImgPesquisa();
        this.imgEmissao = companhiaAereaResponse.getImgEmissao();
        this.imhLogoSmall = companhiaAereaResponse.getImhLogoSmall();
        this.imgLogo = companhiaAereaResponse.getImgLogo();
    }

    public CompanhiaAerea(String iataCia) {
        this.iataCia = iataCia;
    }

    public CompanhiaAerea(Integer codgCompanhiaAerea, String iataCia) {
        this.codgCompanhiaAerea = codgCompanhiaAerea;
        this.iataCia = iataCia;
    }

    public CompanhiaAerea() {
    }

    public CompanhiaAerea(Integer codgCompanhiaAerea) {
        this.codgCompanhiaAerea = codgCompanhiaAerea;
    }

    public Integer getCodgCompanhiaAerea() {
        return codgCompanhiaAerea;
    }

    public void setCodgCompanhiaAerea(Integer codgCompanhiaAerea) {
        this.codgCompanhiaAerea = codgCompanhiaAerea;
    }

    public String getNomeCia() {
        return nomeCia;
    }

    public void setNomeCia(String nomeCia) {
        this.nomeCia = nomeCia;
    }

    public String getIataCia() {
        return iataCia;
    }

    public void setIataCia(String iataCia) {
        this.iataCia = iataCia;
    }

    public String getNumrCia() {
        return numrCia;
    }

    public void setNumrCia(String numrCia) {
        this.numrCia = numrCia;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getFlagReserva() {
        return flagReserva;
    }

    public void setFlagReserva(Integer flagReserva) {
        this.flagReserva = flagReserva;
    }

    public Integer getFlagEmitir() {
        return flagEmitir;
    }

    public void setFlagEmitir(Integer flagEmitir) {
        this.flagEmitir = flagEmitir;
    }

    public String getCodgIntegracao() {
        return codgIntegracao;
    }

    public void setCodgIntegracao(String codgIntegracao) {
        this.codgIntegracao = codgIntegracao;
    }

    public String getImgPesquisa() {
        return imgPesquisa;
    }

    public void setImgPesquisa(String imgPesquisa) {
        this.imgPesquisa = imgPesquisa;
    }

    public String getImgEmissao() {
        return imgEmissao;
    }

    public void setImgEmissao(String imgEmissao) {
        this.imgEmissao = imgEmissao;
    }

    public String getImhLogoSmall() {
        return imhLogoSmall;
    }

    public void setImhLogoSmall(String imhLogoSmall) {
        this.imhLogoSmall = imhLogoSmall;
    }

    public String getImgLogo() {
        return imgLogo;
    }

    public void setImgLogo(String imgLogo) {
        this.imgLogo = imgLogo;
    }
}
