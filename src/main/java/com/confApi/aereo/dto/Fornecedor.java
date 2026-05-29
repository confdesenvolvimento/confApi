package com.confApi.aereo.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Data
public class Fornecedor implements Serializable {

    private Integer cod;
    private String nome;

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + Objects.hashCode(this.cod);
        hash = 73 * hash + Objects.hashCode(this.nome);
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
        final Fornecedor other = (Fornecedor) obj;
        if (!Objects.equals(this.nome, other.nome)) {
            return false;
        }
        return Objects.equals(this.cod, other.cod);
    }
}
