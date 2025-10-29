package com.confApi.db.wooba.checkin.dto;

import java.io.Serializable;
import java.util.Date;

public class TrechoCheckin implements Serializable {

    private int conexao;
    private String de;
    private String dsOrigem;
    private String para;
    private String dsDestino;
    private String companhia;
    private String duracaoVoo;
    private String hora;
    private String voo;
    private String horaRetorno;
    private int status;
    private Date data;
    private int reemissao;
    private int id;
    private String segmento;
    private String statusEmbarque;
    private Boolean horaVoo;


    @Override
    public String toString() {
        String dataFormatada = "";
        if (data != null) {
            dataFormatada = new java.text.SimpleDateFormat("dd/MM/yy").format(data);
        }

        return "Trecho{" +
                "De='" + de + '\'' +
                ", Para='" + para + '\'' +
                ", Companhia='" + companhia + '\'' +
                ", Duração='" + duracaoVoo + '\'' +
                ", Horário='" + hora + '\'' +
                ", Voo='" + voo + '\'' +
                ", Data Viagem='" + dataFormatada + '\'' +
                '}';
    }


    public int getConexao() {
        return conexao;
    }

    public void setConexao(int conexao) {
        this.conexao = conexao;
    }

    public String getDe() {
        return de;
    }

    public void setDe(String de) {
        this.de = de;
    }

    public String getDsOrigem() {
        return dsOrigem;
    }

    public void setDsOrigem(String dsOrigem) {
        this.dsOrigem = dsOrigem;
    }

    public String getPara() {
        return para;
    }

    public void setPara(String para) {
        this.para = para;
    }

    public String getDsDestino() {
        return dsDestino;
    }

    public void setDsDestino(String dsDestino) {
        this.dsDestino = dsDestino;
    }

    public String getCompanhia() {
        return companhia;
    }

    public void setCompanhia(String companhia) {
        this.companhia = companhia;
    }

    public String getDuracaoVoo() {
        return duracaoVoo;
    }

    public void setDuracaoVoo(String duracaoVoo) {
        this.duracaoVoo = duracaoVoo;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getVoo() {
        return voo;
    }

    public void setVoo(String voo) {
        this.voo = voo;
    }

    public String getHoraRetorno() {
        return horaRetorno;
    }

    public void setHoraRetorno(String horaRetorno) {
        this.horaRetorno = horaRetorno;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getData() {
        return data;
    }

    public void setData(Date data) {
        this.data = data;
    }

    public int getReemissao() {
        return reemissao;
    }

    public void setReemissao(int reemissao) {
        this.reemissao = reemissao;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSegmento() {
        return segmento;
    }

    public void setSegmento(String segmento) {
        this.segmento = segmento;
    }

    public String getStatusEmbarque() {
        return statusEmbarque;
    }

    public void setStatusEmbarque(String statusEmbarque) {
        this.statusEmbarque = statusEmbarque;
    }

    public Boolean getHoraVoo() {
        return horaVoo;
    }

    public void setHoraVoo(Boolean horaVoo) {
        this.horaVoo = horaVoo;
    }


}
