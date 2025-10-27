package com.confApi.db.confManager.faturas.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class FaturaSicaRS implements Serializable {

    private int numfat;
    private String dataFatura;
    private int empfat;
    private double valor;
    private String codest;
    private String pago;
    private String dataVen;
    private int contbanco;
    private boolean cancelado;
    private int condicaoPagamento;
    private String invoiceType;
    private String situacao;
    private String fatVenc;
    private Date convertDataVen;
    private Date convertDataFatura;

    public FaturaSicaRS() {
    }

    public FaturaSicaRS(int numfat, String dataFatura, int empfat, double valor, String codest, String pago, String dataVen, int contbanco, boolean cancelado, int condicaoPagamento, String invoiceType, String situacao, String fatVenc) {
        this.numfat = numfat;
        this.dataFatura = dataFatura;
        this.empfat = empfat;
        this.valor = valor;
        this.codest = codest;
        this.pago = pago;
        this.dataVen = dataVen;
        this.contbanco = contbanco;
        this.cancelado = cancelado;
        this.condicaoPagamento = condicaoPagamento;
        this.invoiceType = invoiceType;
        this.situacao = situacao;
        this.fatVenc = fatVenc;
    }



}
