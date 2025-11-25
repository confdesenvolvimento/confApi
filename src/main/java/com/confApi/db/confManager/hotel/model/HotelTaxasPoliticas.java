package com.confApi.db.confManager.hotel.model;

import java.io.Serializable;
import java.util.Objects;

public class HotelTaxasPoliticas implements Serializable{
    private String codgReferencia;
    private String nome;
    private String descricao;
    private Double valor=0.0;

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.codgReferencia);
        hash = 59 * hash + Objects.hashCode(this.nome);
        hash = 59 * hash + Objects.hashCode(this.descricao);
        hash = 59 * hash + Objects.hashCode(this.valor);
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
        final HotelTaxasPoliticas other = (HotelTaxasPoliticas) obj;
        if (!Objects.equals(this.codgReferencia, other.codgReferencia)) {
            return false;
        }
        if (!Objects.equals(this.nome, other.nome)) {
            return false;
        }
        if (!Objects.equals(this.descricao, other.descricao)) {
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


    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }


}

