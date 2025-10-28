package com.confApi.hub.aereo.dto;

import java.util.Date;

public class Passaporte {
    private String nacionalidade;
    private String nomeDoMeioPax;
    private String nomePax;
    private String numero;
    private String paisEmissor;
    private String sobrenomePax;
    private String validade;

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

    public Passaporte(String numero) {
        this.numero = numero;
    }

    public Passaporte() {
    }





    public Passaporte(String nacionalidade, String nomeDoMeioPax, String nomePax,
                      String numero, String paisEmissor, String sobrenomePax, String validade) {
        this.nacionalidade = nacionalidade;
        this.nomeDoMeioPax = nomeDoMeioPax;
        this.nomePax = nomePax;
        this.numero = numero;
        this.paisEmissor = paisEmissor;
        this.sobrenomePax = sobrenomePax;
        this.validade = validade;
    }

    public String getNacionalidade() {
        return nacionalidade;
    }

    public void setNacionalidade(String nacionalidade) {
        this.nacionalidade = nacionalidade;
    }

    public String getNomeDoMeioPax() {
        return nomeDoMeioPax;
    }

    public void setNomeDoMeioPax(String nomeDoMeioPax) {
        this.nomeDoMeioPax = nomeDoMeioPax;
    }

    public String getNomePax() {
        return nomePax;
    }

    public void setNomePax(String nomePax) {
        this.nomePax = nomePax;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getPaisEmissor() {
        return paisEmissor;
    }

    public void setPaisEmissor(String paisEmissor) {
        this.paisEmissor = paisEmissor;
    }

    public String getSobrenomePax() {
        return sobrenomePax;
    }

    public void setSobrenomePax(String sobrenomePax) {
        this.sobrenomePax = sobrenomePax;
    }

    public String getValidade() {
        return validade;
    }

    public void setValidade(String validade) {
        this.validade = validade;
    }


}
