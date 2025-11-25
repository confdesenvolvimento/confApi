package com.confApi.db.confManager.hotel.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class QuartoPesquisa implements Serializable {

    private String id;
    private String nomeQuartoPesquisa;
    private Integer qtdQuartos = 0;
    private Integer qtdAdultos = 0;
    private Integer qtdCriancas = 0;
    private List<Integer> idadeCriancas;
    private HotelAcomodacao acomodacaoSelecionada;
    private List<HotelAcomodacao> acomodacoes = new ArrayList<>();

    public void selecionaAcomodacao(HotelAcomodacao ac) {
        for (HotelAcomodacao acc : acomodacoes) {
            if (ac.getIdUnico().equalsIgnoreCase(acc.getIdUnico())) {
                ac.setIsSelecionado(Boolean.TRUE);
                acomodacaoSelecionada = ac;
            } else {
                acc.setIsSelecionado(Boolean.FALSE);
            }
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.id);
        hash = 67 * hash + Objects.hashCode(this.nomeQuartoPesquisa);
        hash = 67 * hash + Objects.hashCode(this.qtdQuartos);
        hash = 67 * hash + Objects.hashCode(this.qtdAdultos);
        hash = 67 * hash + Objects.hashCode(this.qtdCriancas);
        hash = 67 * hash + Objects.hashCode(this.idadeCriancas);
        hash = 67 * hash + Objects.hashCode(this.acomodacaoSelecionada);
        hash = 67 * hash + Objects.hashCode(this.acomodacoes);
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
        final QuartoPesquisa other = (QuartoPesquisa) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.nomeQuartoPesquisa, other.nomeQuartoPesquisa)) {
            return false;
        }
        if (!Objects.equals(this.qtdQuartos, other.qtdQuartos)) {
            return false;
        }
        if (!Objects.equals(this.qtdAdultos, other.qtdAdultos)) {
            return false;
        }
        if (!Objects.equals(this.qtdCriancas, other.qtdCriancas)) {
            return false;
        }
        if (!Objects.equals(this.idadeCriancas, other.idadeCriancas)) {
            return false;
        }
        if (!Objects.equals(this.acomodacaoSelecionada, other.acomodacaoSelecionada)) {
            return false;
        }
        return Objects.equals(this.acomodacoes, other.acomodacoes);
    }

    public HotelAcomodacao getAcomodacaoSelecionada() {
        return acomodacaoSelecionada;
    }

    public Integer getQtdQuartos() {
        return qtdQuartos;
    }

    public void setQtdQuartos(Integer qtdQuartos) {
        this.qtdQuartos = qtdQuartos;
    }

    public void setAcomodacaoSelecionada(HotelAcomodacao acomodacaoSelecionada) {
        this.acomodacaoSelecionada = acomodacaoSelecionada;
    }

    public String getNomeQuartoPesquisa() {
        return nomeQuartoPesquisa;
    }

    public void setNomeQuartoPesquisa(String nomeQuartoPesquisa) {
        this.nomeQuartoPesquisa = nomeQuartoPesquisa;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getQtdAdultos() {
        return qtdAdultos;
    }

    public void setQtdAdultos(Integer qtdAdultos) {
        this.qtdAdultos = qtdAdultos;
    }

    public Integer getQtdCriancas() {
        return qtdCriancas;
    }

    public void setQtdCriancas(Integer qtdCriancas) {
        this.qtdCriancas = qtdCriancas;
    }

    public List<Integer> getIdadeCriancas() {
        return idadeCriancas;
    }

    public void setIdadeCriancas(List<Integer> idadeCriancas) {
        this.idadeCriancas = idadeCriancas;
    }

    public List<HotelAcomodacao> getAcomodacoes() {
        return acomodacoes;
    }

    public void setAcomodacoes(List<HotelAcomodacao> acomodacoes) {
        this.acomodacoes = acomodacoes;
    }

}

