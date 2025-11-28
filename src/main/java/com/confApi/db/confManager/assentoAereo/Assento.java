package com.confApi.db.confManager.assentoAereo;

import com.confApi.endPoints.assento.AssentoResponse;

import java.io.Serializable;
import java.util.Objects;

public class Assento implements Serializable {

    private String assentoColuna;
    private String assentoLinha;
    private String paxID;
    private String trechoID;
    private String voo;
    private String passageiro;
    private Double valor;
    private String trecho;

    public Assento(AssentoResponse assentoResponse) {
        this.assentoColuna = assentoResponse.getAssentoColuna();
        this.assentoLinha = assentoResponse.getAssentoLinha();
        this.paxID = assentoResponse.getPaxID();
        this.trechoID = assentoResponse.getTrechoID();
        this.voo = assentoResponse.getVoo();
        this.passageiro = assentoResponse.getPassageiro();
        this.valor = assentoResponse.getValor();
        this.trecho = assentoResponse.toString();
    }

    public Assento() {
    }

    public Assento(String assentoColuna, String assentoLinha, String paxID, String trechoID) {
        this.assentoColuna = assentoColuna;
        this.assentoLinha = assentoLinha;
        this.paxID = paxID;
        this.trechoID = trechoID;
    }

    public Assento(String assentoColuna, String assentoLinha, String paxID, String trechoID,
                   String voo, String passageiro, Double valor, String trecho) {
        this.assentoColuna = assentoColuna;
        this.assentoLinha = assentoLinha;
        this.paxID = paxID;
        this.trechoID = trechoID;
        this.voo = voo;
        this.passageiro = passageiro;
        this.valor = valor;
        this.trecho = trecho;
    }

    public String getAssentoColuna() {
        return assentoColuna;
    }

    public void setAssentoColuna(String assentoColuna) {
        this.assentoColuna = assentoColuna;
    }

    public String getAssentoLinha() {
        return assentoLinha;
    }

    public void setAssentoLinha(String assentoLinha) {
        this.assentoLinha = assentoLinha;
    }

    public String getPaxID() {
        return paxID;
    }

    public void setPaxID(String paxID) {
        this.paxID = paxID;
    }

    public String getTrechoID() {
        return trechoID;
    }

    public void setTrechoID(String trechoID) {
        this.trechoID = trechoID;
    }

    public String getVoo() {
        return voo;
    }

    public void setVoo(String voo) {
        this.voo = voo;
    }

    public String getPassageiro() {
        return passageiro;
    }

    public void setPassageiro(String passageiro) {
        this.passageiro = passageiro;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public String getTrecho() {
        return trecho;
    }

    public void setTrecho(String trecho) {
        this.trecho = trecho;
    }

    @Override
    public String toString() {
        return "Assento{" + "assentoColuna=" + assentoColuna + ", assentoLinha=" + assentoLinha + ", paxID=" + paxID + ", trechoID=" + trechoID + ", voo=" + voo + ", passageiro=" + passageiro + ", valor=" + valor + ", trecho=" + trecho + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.assentoColuna);
        hash = 53 * hash + Objects.hashCode(this.assentoLinha);
        hash = 53 * hash + Objects.hashCode(this.paxID);
        hash = 53 * hash + Objects.hashCode(this.trechoID);
        hash = 53 * hash + Objects.hashCode(this.voo);
        hash = 53 * hash + Objects.hashCode(this.passageiro);
        hash = 53 * hash + Objects.hashCode(this.valor);
        hash = 53 * hash + Objects.hashCode(this.trecho);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Assento other = (Assento) obj;
        if (!Objects.equals(this.assentoColuna, other.assentoColuna)) {
            return false;
        }
        if (!Objects.equals(this.assentoLinha, other.assentoLinha)) {
            return false;
        }
        if (!Objects.equals(this.paxID, other.paxID)) {
            return false;
        }
        if (!Objects.equals(this.trechoID, other.trechoID)) {
            return false;
        }
        if (!Objects.equals(this.voo, other.voo)) {
            return false;
        }
        if (!Objects.equals(this.passageiro, other.passageiro)) {
            return false;
        }
        if (!Objects.equals(this.trecho, other.trecho)) {
            return false;
        }
        return Objects.equals(this.valor, other.valor);
    }


}
