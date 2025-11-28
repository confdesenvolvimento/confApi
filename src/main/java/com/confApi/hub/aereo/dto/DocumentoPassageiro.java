package com.confApi.hub.aereo.dto;

import com.confApi.endPoints.documentoPassageiro.DocumentoPassageiroResponse;

import java.util.Date;

public class DocumentoPassageiro {

    private String nacionalidade = "";
    private String numero;
    private String paisEmissor;
    private Integer tipo = 1;
    private String validade;

    public Date converterData(String data) {
        String timestampString = data.substring(data.indexOf("(") + 1, data.indexOf("-"));
        if (!timestampString.equalsIgnoreCase("")) {
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
        } else {
            return null;
        }
    }

    public DocumentoPassageiro(DocumentoPassageiroResponse documentoPassageiroResponse) {
        this.nacionalidade = documentoPassageiroResponse.getNacionalidade();
        this.numero = documentoPassageiroResponse.getNumero();
        this.paisEmissor = documentoPassageiroResponse.getPaisEmissor();
        this.tipo = documentoPassageiroResponse.getTipo();
        this.validade = documentoPassageiroResponse.getValidade();
    }

    public DocumentoPassageiro() {
    }

    public DocumentoPassageiro(String nacionalidade, String numero, String paisEmissor,
                               Integer tipo) {
        this.nacionalidade = nacionalidade;
        this.numero = numero;
        this.paisEmissor = paisEmissor;
        this.tipo = tipo;

    }

    public String getValidade() {
        return validade;
    }

    public void setValidade(String validade) {
        this.validade = validade;
    }

    public String getNacionalidade() {
        return nacionalidade;
    }

    public void setNacionalidade(String nacionalidade) {
        this.nacionalidade = nacionalidade;
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

    public Integer getTipo() {
        return tipo;
    }

    public void setTipo(Integer tipo) {
        this.tipo = tipo;
    }

}


