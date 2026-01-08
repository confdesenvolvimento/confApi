package com.confApi.seguros.model;

import java.io.Serializable;

public class DestinoSeguro implements Serializable {

    private String codigo; // ex: "US_CA", "EUROPA", "MUNDO"
    private String label;  // ex: "Estados Unidos e Canadá"

    public DestinoSeguro() {}

    public DestinoSeguro(String codigo, String label) {
        this.codigo = codigo;
        this.label = label;
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
