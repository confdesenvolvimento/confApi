package com.confApi.db.wooba.checkin.dto.ia;

import lombok.Data;

@Data
public class  TrechoCheckInIA {
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
    private long data; // Timestamp em milissegundos
    private int reemissao;
    private int id;
    private String segmento;
    private String statusEmbarque;
    private String horaVoo;

    @Override
    public String toString() {
        return "Trecho{" +
                "De='" + de + '\'' +
                ", Para='" + para + '\'' +
                ", Companhia='" + companhia + '\'' +
                ", Duração='" + duracaoVoo + '\'' +
                ", Horário='" + hora + '\'' +
                ", Voo='" + voo + '\'' +
                ", Data=" + new java.util.Date(data) + // Converter timestamp para data legível
                '}';
    }
}
