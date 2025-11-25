package com.confApi.db.confManager.hotel.model;

import java.io.Serializable;
import java.util.Objects;

public class HotelDetalhesHotel implements Serializable {
    private Integer codgHotelDetalhe;
    private String codgReferencia;
    private String nome;
    private String descricao;
    private Integer referenciaIcone =1;

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.codgHotelDetalhe);
        hash = 89 * hash + Objects.hashCode(this.codgReferencia);
        hash = 89 * hash + Objects.hashCode(this.nome);
        hash = 89 * hash + Objects.hashCode(this.descricao);
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
        final HotelDetalhesHotel other = (HotelDetalhesHotel) obj;
        if (!Objects.equals(this.codgReferencia, other.codgReferencia)) {
            return false;
        }
        if (!Objects.equals(this.nome, other.nome)) {
            return false;
        }
        if (!Objects.equals(this.descricao, other.descricao)) {
            return false;
        }
        return Objects.equals(this.codgHotelDetalhe, other.codgHotelDetalhe);
    }



    public Integer getCodgHotelDetalhe() {
        return codgHotelDetalhe;
    }

    public void setCodgHotelDetalhe(Integer codgHotelDetalhe) {
        this.codgHotelDetalhe = codgHotelDetalhe;
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

    @Override
    public String toString() {
        return "HotelDetalhesHotel{" + "codgHotelDetalhe=" + codgHotelDetalhe + ", codgReferencia=" + codgReferencia + ", nome=" + nome + ", descricao=" + descricao + '}';
    }

    public Integer getReferenciaIcone() {
        return referenciaIcone;
    }

    public void setReferenciaIcone(Integer referenciaIcone) {
        this.referenciaIcone = referenciaIcone;
    }

    public String getIconeComodidade(String nome) {
        if (nome == null) return "pi pi-info-circle";

        String nomeLower = nome.toLowerCase();

        if (nomeLower.contains("estacionamento")) return "pi pi-car";
        if (nomeLower.contains("garagem")) return "pi pi-car";
        if (nomeLower.contains("internet") || nomeLower.contains("wifi")) return "pi pi-wifi";
        if (nomeLower.contains("lavanderia")) return "pi pi-refresh";
        if (nomeLower.contains("cartão") || nomeLower.contains("cartões")) return "pi pi-credit-card";
        if (nomeLower.contains("elevador")) return "pi pi-sort-amount-up-alt";
        if (nomeLower.contains("auditório")) return "pi pi-users";
        if (nomeLower.contains("fax")) return "pi pi-phone";
        if (nomeLower.contains("Impressão")) return "pi pi-print";
        if (nomeLower.contains("Guarda de Bagagem")) return "pi pi-shopping-bag";
        if (nomeLower.contains("Recepção 24 horas")) return "pi pi-bell";
        if (nomeLower.contains("acessibilidade")) return "pi pi-wheelchair";



        return "pi pi-info-circle"; // padrão
    }

}

