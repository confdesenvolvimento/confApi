package com.confApi.hub.aereo.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Bilhete {
    private Boolean bilheteDoInfantil;
    private Date dataDeEmissao;
    private String numero;
    private List<Pagamento> pagamentos;
    private String passageiro;
    private List<Passageiro> passageirosAdicionais;
    private String paxRef;
    private String status;
    private List<Voo> voos;

    public Date converterData(String data){
        String timestampString = data.substring(data.indexOf("(") + 1, data.indexOf("-"));
        if(!timestampString.equalsIgnoreCase("")) {
            String offsetString = data.substring(data.indexOf("-") + 1, data.indexOf(")"));
            long timestamp = Long.parseLong(timestampString);
            int offsetHours = Integer.parseInt(offsetString.substring(0, 3));
            int offsetMinutes = Integer.parseInt(offsetString.substring(3));

            // Calcular o deslocamento em milissegundos
            int offsetMilliseconds = (offsetHours * 60 + offsetMinutes) * 60 * 1000;

            // Somar o deslocamento ao timestamp
            Date date = new Date(timestamp + offsetMilliseconds);

            // Imprimir a data convertida
            return date;
        } else{
            return null;
        }
    }



    public Bilhete() {
    }

    public Bilhete(Boolean bilheteDoInfantil, Date dataDeEmissao, String numero,
                   List<Pagamento> pagamentos, String passageiro, List<Passageiro> passageirosAdicionais,
                   String paxRef, String status, List<Voo> voos) {
        this.bilheteDoInfantil = bilheteDoInfantil;
        this.dataDeEmissao = dataDeEmissao;
        this.numero = numero;
        this.pagamentos = pagamentos;
        this.passageiro = passageiro;
        this.passageirosAdicionais = passageirosAdicionais;
        this.paxRef = paxRef;
        this.status = status;
        this.voos = voos;
    }

    public Boolean getBilheteDoInfantil() {
        return bilheteDoInfantil;
    }

    public void setBilheteDoInfantil(Boolean bilheteDoInfantil) {
        this.bilheteDoInfantil = bilheteDoInfantil;
    }

    public Date getDataDeEmissao() {
        return dataDeEmissao;
    }

    public void setDataDeEmissao(Date dataDeEmissao) {
        this.dataDeEmissao = dataDeEmissao;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public List<Pagamento> getPagamentos() {
        return pagamentos;
    }

    public void setPagamentos(List<Pagamento> pagamentos) {
        this.pagamentos = pagamentos;
    }

    public String getPassageiro() {
        return passageiro;
    }

    public void setPassageiro(String passageiro) {
        this.passageiro = passageiro;
    }

    public List<Passageiro> getPassageirosAdicionais() {
        return passageirosAdicionais;
    }

    public void setPassageirosAdicionais(List<Passageiro> passageirosAdicionais) {
        this.passageirosAdicionais = passageirosAdicionais;
    }

    public String getPaxRef() {
        return paxRef;
    }

    public void setPaxRef(String paxRef) {
        this.paxRef = paxRef;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Voo> getVoos() {
        return voos;
    }

    public void setVoos(List<Voo> voos) {
        this.voos = voos;
    }
}

