package com.confApi.db.confManager.faturas.dto;

import lombok.Data;

@Data
public class FaturaRQ {

    private Integer invoiceNumber;
    private String empfat;
    private int condpag;
    private double valor;
    private String dataVen;

    public FaturaRQ(Integer invoiceNumber, String empfat, int condpag, double valor, String dataVen) {
        this.invoiceNumber = invoiceNumber;
        this.empfat = empfat;
        this.condpag = condpag;
        this.valor = valor;
        this.dataVen = dataVen;
    }

}
