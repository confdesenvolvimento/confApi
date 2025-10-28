package com.confApi.db.confManager.bandeira;


import java.io.Serializable;

public class Bandeira implements Serializable {

    private Integer codgBandeira;
    private String nomeBandeira;
    private String siglaBandeira;

    public Bandeira() {
    }

    public Bandeira(Integer codgBandeira, String nomeBandeira, String siglaBandeira) {
        this.codgBandeira = codgBandeira;
        this.nomeBandeira = nomeBandeira;
        this.siglaBandeira = siglaBandeira;
    }

    public Bandeira(Integer codgBandeira) {
        this.codgBandeira = codgBandeira;
    }

    public Integer getCodgBandeira() {
        return codgBandeira;
    }

    public void setCodgBandeira(Integer codgBandeira) {
        this.codgBandeira = codgBandeira;
    }

    public String getNomeBandeira() {
        return nomeBandeira;
    }

    public void setNomeBandeira(String nomeBandeira) {
        this.nomeBandeira = nomeBandeira;
    }

    public String getSiglaBandeira() {
        return siglaBandeira;
    }

    public void setSiglaBandeira(String siglaBandeira) {
        this.siglaBandeira = siglaBandeira;
    }

    @Override
    public String toString() {
        return "Bandeira{" + "codgBandeira=" + codgBandeira + ", nomeBandeira=" + nomeBandeira + ", siglaBandeira=" + siglaBandeira + '}';
    }


}
