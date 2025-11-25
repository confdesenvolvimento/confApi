package com.confApi.hub.hotel.dto;

import java.io.Serializable;
import java.util.Objects;

public class HotelPoliticaCancelamento implements Serializable{
    private Double amount = 0.0;
    private Double percent = 0.0;
    private String inicio;
    private String fim;
    private String dataLimite;
    private String duracao;
    private Boolean naoReembolsavel;
    private String descricaoPenalidade;
    private String nome;

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(this.amount);
        hash = 67 * hash + Objects.hashCode(this.percent);
        hash = 67 * hash + Objects.hashCode(this.inicio);
        hash = 67 * hash + Objects.hashCode(this.fim);
        hash = 67 * hash + Objects.hashCode(this.dataLimite);
        hash = 67 * hash + Objects.hashCode(this.duracao);
        hash = 67 * hash + Objects.hashCode(this.naoReembolsavel);
        hash = 67 * hash + Objects.hashCode(this.descricaoPenalidade);
        hash = 67 * hash + Objects.hashCode(this.nome);
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
        final HotelPoliticaCancelamento other = (HotelPoliticaCancelamento) obj;
        if (!Objects.equals(this.inicio, other.inicio)) {
            return false;
        }
        if (!Objects.equals(this.fim, other.fim)) {
            return false;
        }
        if (!Objects.equals(this.dataLimite, other.dataLimite)) {
            return false;
        }
        if (!Objects.equals(this.duracao, other.duracao)) {
            return false;
        }
        if (!Objects.equals(this.descricaoPenalidade, other.descricaoPenalidade)) {
            return false;
        }
        if (!Objects.equals(this.nome, other.nome)) {
            return false;
        }
        if (!Objects.equals(this.amount, other.amount)) {
            return false;
        }
        if (!Objects.equals(this.percent, other.percent)) {
            return false;
        }
        return Objects.equals(this.naoReembolsavel, other.naoReembolsavel);
    }




    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getPercent() {
        return percent;
    }

    public void setPercent(Double percent) {
        this.percent = percent;
    }

    public String getInicio() {
        return inicio;
    }

    public void setInicio(String inicio) {
        this.inicio = inicio;
    }

    public String getFim() {
        return fim;
    }

    public void setFim(String fim) {
        this.fim = fim;
    }

    public String getDataLimite() {
        return dataLimite;
    }

    public void setDataLimite(String dataLimite) {
        this.dataLimite = dataLimite;
    }

    public String getDuracao() {
        return duracao;
    }

    public void setDuracao(String duracao) {
        this.duracao = duracao;
    }

    public Boolean getNaoReembolsavel() {
        return naoReembolsavel;
    }

    public void setNaoReembolsavel(Boolean naoReembolsavel) {
        this.naoReembolsavel = naoReembolsavel;
    }

    public String getDescricaoPenalidade() {
        return descricaoPenalidade;
    }

    public void setDescricaoPenalidade(String descricaoPenalidade) {
        this.descricaoPenalidade = descricaoPenalidade;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }


}

