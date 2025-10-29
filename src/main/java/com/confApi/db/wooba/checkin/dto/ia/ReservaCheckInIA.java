package com.confApi.db.wooba.checkin.dto.ia;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
@Data
public class ReservaCheckInIA implements Serializable {

    private String id;
    private String trecho;
    private String nomeAgencia;
    private String nomeUnidade;
    private String numeroDoBilhete;
    private String numeroBilheteOriginal;
    private String tktBilhete;
    private String logomarca;
    private String localizadorCompanhia;
    private long data; // Timestamp em milissegundos
    private String companhia;
    private String nomeCompleto;
    private String email;
    private String passageiro;
    private int reemissao;
    private int statusReserva;
    private String linkCheckin;
    private List<TrechoCheckInIA> trechosIda;
    private List<TrechoCheckInIA> trechosVolta;
    private List<TrechoCheckInIA> trechosMultiplaConexao;
    private String nomeCia;

    @Override
    public String toString() {
        return "Reserva{"
                + "ID='" + id + '\''
                + ", Trecho='" + trecho + '\''
                + ", Agência='" + nomeAgencia + '\''
                + ", Unidade='" + nomeUnidade + '\''
                + ", Número do Bilhete='" + numeroDoBilhete + '\''
                + ", Companhia='" + companhia + '\''
                + ", Nome Completo='" + nomeCompleto + '\''
                + ", Passageiro='" + passageiro + '\''
                + ", Data=" + new java.util.Date(data)
                + // Converter timestamp para data legível
                ", Link Check-in=" + gerarCommandLink(linkCheckin)
                + ", Trechos: " + trechosMultiplaConexao
                + '}';
    }

    private String gerarCommandLink(String url) {
        if (url == null || url.isEmpty()) {
            return "Sem link disponível";
        }
        return "<h:commandLink action=\"#{chatBean.redirecionarParaLink('" + url + "')}\" value=\"Clique aqui para fazer o Check-in\" />";
    }

}
