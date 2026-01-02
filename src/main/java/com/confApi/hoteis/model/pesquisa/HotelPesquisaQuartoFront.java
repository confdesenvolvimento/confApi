package com.confApi.hoteis.model.pesquisa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class HotelPesquisaQuartoFront implements Serializable {
    private int id = 0;
    private String nomeQuartoPesquisa;
    private Integer qtdQuartos = 1;
    private Integer qtdAdultos = 1;
    private Integer qtdCriancas = 0;
    private List<HotelPesquisaIdadeCriancaFront> idadeCriancas = new ArrayList<HotelPesquisaIdadeCriancaFront>();



    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }



    public String getNomeQuartoPesquisa() {
        return nomeQuartoPesquisa;
    }

    public void setNomeQuartoPesquisa(String nomeQuartoPesquisa) {
        this.nomeQuartoPesquisa = nomeQuartoPesquisa;
    }

    public Integer getQtdQuartos() {
        return qtdQuartos;
    }

    public void setQtdQuartos(Integer qtdQuartos) {
        this.qtdQuartos = qtdQuartos;
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

    public List<HotelPesquisaIdadeCriancaFront> getIdadeCriancas() {
        return idadeCriancas;
    }

    public void setIdadeCriancas(List<HotelPesquisaIdadeCriancaFront> idadeCriancas) {
        this.idadeCriancas = idadeCriancas;
    }
}
