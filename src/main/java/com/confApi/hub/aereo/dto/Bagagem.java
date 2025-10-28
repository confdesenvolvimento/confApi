package com.confApi.hub.aereo.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author ti
 */
public class Bagagem implements Serializable{
    private String id;
    private Integer quantidade;
    private Double valor;
    private Integer peso;
    private String unidadeDeMedida;

    public Bagagem() {
    }

    public Bagagem(String id, Integer quantidade, Double valor, Integer peso, String unidadeDeMedida) {
        this.id = id;
        this.quantidade = quantidade;
        this.valor = valor;
        this.peso = peso;
        this.unidadeDeMedida = unidadeDeMedida;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Integer getPeso() {
        return peso;
    }

    public void setPeso(Integer peso) {
        this.peso = peso;
    }

    public String getUnidadeDeMedida() {
        return unidadeDeMedida;
    }

    public void setUnidadeDeMedida(String unidadeDeMedida) {
        this.unidadeDeMedida = unidadeDeMedida;
    }

    @Override
    public String toString() {
        return "Bagagem{" + "id=" + id + ", quantidade=" + quantidade + ", valor=" + valor + ", peso=" + peso + ", unidadeDeMedida=" + unidadeDeMedida + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
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
        final Bagagem other = (Bagagem) obj;
        return Objects.equals(this.id, other.id);
    }


}
