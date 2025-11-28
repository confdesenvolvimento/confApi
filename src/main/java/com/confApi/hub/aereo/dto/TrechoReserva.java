package com.confApi.hub.aereo.dto;

import com.confApi.endPoints.trechoReserva.TrechoReservaResponse;
import com.confApi.endPoints.voo.VooResponse;

import java.util.ArrayList;
import java.util.List;

public class TrechoReserva {
    private String sistema;
    private Companhia companhia;
    private Aeroporto destino;
    private Integer duracao;
    private Integer numeroParadas;
    private Aeroporto origem;
    private List<Voo> voos;
    private String tempoDeDuracao;
    private String identificacaoDaViagem;

    public TrechoReserva(TrechoReservaResponse trechoReservaResponse) {
        this.sistema = trechoReservaResponse.getSistema();
        this.companhia = new Companhia(trechoReservaResponse.getCompanhia());
        this.destino = new Aeroporto(trechoReservaResponse.getDestino());
        this.duracao = trechoReservaResponse.getDuracao();
        this.numeroParadas = trechoReservaResponse.getNumeroParadas();
        this.origem = new Aeroporto(trechoReservaResponse.getOrigem());
        this.voos = new ArrayList<>();
        for (VooResponse vooResponse : trechoReservaResponse.getVoos()) {
            this.voos.add(new Voo(vooResponse));
        }
        this.tempoDeDuracao = trechoReservaResponse.getTempoDeDuracao();
        this.identificacaoDaViagem = trechoReservaResponse.getIdentificacaoDaViagem();
    }

    public TrechoReserva() {
    }

    public TrechoReserva(String sistema, Companhia companhia, Aeroporto destino, Integer duracao,
                         Integer numeroParadas, Aeroporto origem, List<Voo> voos,
                         String tempoDeDuracao, String identificacaoDaViagem) {
        this.sistema = sistema;
        this.companhia = companhia;
        this.destino = destino;
        this.duracao = duracao;
        this.numeroParadas = numeroParadas;
        this.origem = origem;
        this.voos = voos;
        this.tempoDeDuracao = tempoDeDuracao;
        this.identificacaoDaViagem = identificacaoDaViagem;
    }

    public String getSistema() {
        return sistema;
    }

    public void setSistema(String sistema) {
        this.sistema = sistema;
    }

    public Companhia getCompanhia() {
        return companhia;
    }

    public void setCompanhia(Companhia companhia) {
        this.companhia = companhia;
    }

    public Aeroporto getDestino() {
        return destino;
    }

    public void setDestino(Aeroporto destino) {
        this.destino = destino;
    }

    public Integer getDuracao() {
        return duracao;
    }

    public void setDuracao(Integer duracao) {
        this.duracao = duracao;
    }

    public Integer getNumeroParadas() {
        return numeroParadas;
    }

    public void setNumeroParadas(Integer numeroParadas) {
        this.numeroParadas = numeroParadas;
    }

    public Aeroporto getOrigem() {
        return origem;
    }

    public void setOrigem(Aeroporto origem) {
        this.origem = origem;
    }

    public List<Voo> getVoos() {
        return voos;
    }

    public void setVoos(List<Voo> voos) {
        this.voos = voos;
    }

    public String getTempoDeDuracao() {
        return tempoDeDuracao;
    }

    public void setTempoDeDuracao(String tempoDeDuracao) {
        this.tempoDeDuracao = tempoDeDuracao;
    }

    public String getIdentificacaoDaViagem() {
        return identificacaoDaViagem;
    }

    public void setIdentificacaoDaViagem(String identificacaoDaViagem) {
        this.identificacaoDaViagem = identificacaoDaViagem;
    }

    @Override
    public String toString() {
        return "Trecho{" +
                "sistema='" + sistema + '\'' +
                ", companhia=" + companhia +
                ", destino=" + destino +
                ", duracao=" + duracao +
                ", numeroParadas=" + numeroParadas +
                ", origem=" + origem +
                ", voos=" + voos +
                ", tempoDeDuracao='" + tempoDeDuracao + '\'' +
                ", identificacaoDaViagem='" + identificacaoDaViagem + '\'' +
                '}';
    }
}
