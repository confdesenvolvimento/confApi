package com.confApi.db.confManager.hotel.model;

import java.io.Serializable;
import java.util.Objects;

public class HotelServicoHotel implements Serializable {
    private String codgReferencia;
    private String nomeServico;
    private String descricaoServico;
    private Double valor =0.0;

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.codgReferencia);
        hash = 29 * hash + Objects.hashCode(this.nomeServico);
        hash = 29 * hash + Objects.hashCode(this.descricaoServico);
        hash = 29 * hash + Objects.hashCode(this.valor);
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
        final HotelServicoHotel other = (HotelServicoHotel) obj;
        if (!Objects.equals(this.codgReferencia, other.codgReferencia)) {
            return false;
        }
        if (!Objects.equals(this.nomeServico, other.nomeServico)) {
            return false;
        }
        if (!Objects.equals(this.descricaoServico, other.descricaoServico)) {
            return false;
        }
        return Objects.equals(this.valor, other.valor);
    }



    public String getCodgReferencia() {
        return codgReferencia;
    }

    public void setCodgReferencia(String codgReferencia) {
        this.codgReferencia = codgReferencia;
    }



    public String getNomeServico() {
        return nomeServico;
    }

    public void setNomeServico(String nomeServico) {
        this.nomeServico = nomeServico;
    }

    public String getDescricaoServico() {
        return descricaoServico;
    }

    public void setDescricaoServico(String descricaoServico) {
        this.descricaoServico = descricaoServico;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }


}

