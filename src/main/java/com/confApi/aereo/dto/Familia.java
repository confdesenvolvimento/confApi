package com.confApi.aereo.dto;

import java.io.Serializable;
import java.util.Objects;

public class Familia implements Serializable {
    private String descricaoFamilia;
    private String codgFamilia;



    public Familia() {
    }


    public Familia(String descricaoFamilia, String codgFamilia) {
        this.descricaoFamilia = descricaoFamilia;
        this.codgFamilia = codgFamilia;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.descricaoFamilia);
        hash = 97 * hash + Objects.hashCode(this.codgFamilia);
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
        final Familia other = (Familia) obj;
        if (!Objects.equals(this.descricaoFamilia, other.descricaoFamilia)) {
            return false;
        }
        return Objects.equals(this.codgFamilia, other.codgFamilia);
    }



    public String getDescricaoFamilia() {
        return descricaoFamilia;
    }

    public void setDescricaoFamilia(String descricaoFamilia) {
        this.descricaoFamilia = descricaoFamilia;
    }

    public String getCodgFamilia() {
        return codgFamilia;
    }

    public void setCodgFamilia(String codgFamilia) {
        this.codgFamilia = codgFamilia;
    }
}
