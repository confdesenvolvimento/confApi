package com.confApi.db.confManager.faturas.dto;

import java.io.Serializable;

public class FaturaIA implements Serializable {

    private int numfat;
    private String dataFatura;
    private int empfat;
    private double valor;
    private String codest;
    private String pago;
    private String dataVen;
    private int contbanco;
    private boolean cancelado = false;
    private String canceladoDesc;
    private int condicaoPagamento;
    private String invoiceType;
    private String situacao;
    private String fatVenc;
    private String convertDataVen;
    private String convertDataFatura;

    public String getPago() {
        // Mapear tipoLimite para "Forma de pagamento"
        if (pago.equalsIgnoreCase("Não") && fatVenc.equalsIgnoreCase("Sim")) {
            pago = "VENCIDA";
        } else if (pago.equalsIgnoreCase("Não") && fatVenc.equalsIgnoreCase("Não")) {
            pago = "EM ABERTO";
        } else if (pago.equalsIgnoreCase("Sim")) {
            pago = "PAGA";
        }
        return pago;
    }
    public String getSituacao() {
        if (situacao.equalsIgnoreCase("Faturada")) {
            situacao = "FATURA DÉBITO";
        } else if (situacao.equalsIgnoreCase("Faturada Crédito")) {
            situacao = "FATURA CRÉDITO";
        } else if (pago.equalsIgnoreCase("À Faturar")) {
            situacao = "À FATURAR";
        }
        return situacao;
    }

    public boolean isCancelado() {
        return cancelado;
    }

    public String getCanceladoDesc() {
        if(cancelado){
            canceladoDesc = "Sim";
        }else{
            canceladoDesc = "Não";
        }
        return canceladoDesc;
    }

    public void setCanceladoDesc(String canceladoDesc) {
        this.canceladoDesc = canceladoDesc;
    }


    @Override
    public String toString() {
        return "Fatura{"
                + "numfat=" + numfat
                + ", dataFatura='" + dataFatura + '\''
                + ", empfat=" + empfat
                + ", valor=" + valor
                + ", codest='" + codest + '\''
                + ", pago='" + pago + '\''
                + ", dataVen='" + dataVen + '\''
                + ", contbanco=" + contbanco
                + ", cancelado=" + cancelado
                + ", condicaoPagamento=" + condicaoPagamento
                + ", invoiceType='" + invoiceType + '\''
                + ", Tipo de Fatura='" + situacao + '\''
                + ", fatVenc='" + fatVenc + '\''
                + '}';
    }

    public String getConvertDataVen() {
        return convertDataVen;
    }

    public void setConvertDataVen(String convertDataVen) {
        this.convertDataVen = convertDataVen;
    }


    public int getNumfat() {
        return numfat;
    }

    public void setNumfat(int numfat) {
        this.numfat = numfat;
    }

    public String getDataFatura() {
        return dataFatura;
    }

    public void setDataFatura(String dataFatura) {
        this.dataFatura = dataFatura;
    }

    public int getEmpfat() {
        return empfat;
    }

    public void setEmpfat(int empfat) {
        this.empfat = empfat;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public String getCodest() {
        return codest;
    }

    public void setCodest(String codest) {
        this.codest = codest;
    }

    public void setPago(String pago) {
        this.pago = pago;
    }

    public String getDataVen() {
        return dataVen;
    }

    public void setDataVen(String dataVen) {
        this.dataVen = dataVen;
    }

    public int getContbanco() {
        return contbanco;
    }

    public void setContbanco(int contbanco) {
        this.contbanco = contbanco;
    }

    public String getConvertDataFatura() {
        return convertDataFatura;
    }

    public void setConvertDataFatura(String convertDataFatura) {
        this.convertDataFatura = convertDataFatura;
    }



    public void setCancelado(boolean cancelado) {
        this.cancelado = cancelado;
    }

    public int getCondicaoPagamento() {
        return condicaoPagamento;
    }

    public void setCondicaoPagamento(int condicaoPagamento) {
        this.condicaoPagamento = condicaoPagamento;
    }

    public String getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(String invoiceType) {
        this.invoiceType = invoiceType;
    }



    public void setSituacao(String situacao) {
        this.situacao = situacao;
    }

    public String getFatVenc() {
        return fatVenc;
    }

    public void setFatVenc(String fatVenc) {
        this.fatVenc = fatVenc;
    }

}