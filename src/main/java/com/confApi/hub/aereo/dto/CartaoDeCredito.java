package com.confApi.hub.aereo.dto;

public class CartaoDeCredito {
    private Integer id;
    private String autorizacao;
    private String bandeira;
    private String codigoDeSeguranca;
    private Boolean ignorarValidacao;
    private String numero;
    private String titularCPF;
    private String titularNome;
    private String validade;
    private Integer parcelas;
    private Integer financiamentoPagamento;



    public CartaoDeCredito() {
    }

    public CartaoDeCredito(Integer id, String autorizacao, String bandeira,
                           String codigoDeSeguranca, Boolean ignorarValidacao,
                           String numero, String titularCPF, String titularNome,
                           String validade, Integer parcelas, Integer financiamentoPagamento) {
        this.id = id;
        this.autorizacao = autorizacao;
        this.bandeira = bandeira;
        this.codigoDeSeguranca = codigoDeSeguranca;
        this.ignorarValidacao = ignorarValidacao;
        this.numero = numero;
        this.titularCPF = titularCPF;
        this.titularNome = titularNome;
        this.validade = validade;
        this.parcelas = parcelas;
        this.financiamentoPagamento = financiamentoPagamento;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAutorizacao() {
        return autorizacao;
    }

    public void setAutorizacao(String autorizacao) {
        this.autorizacao = autorizacao;
    }

    public String getBandeira() {
        return bandeira;
    }

    public void setBandeira(String bandeira) {
        this.bandeira = bandeira;
    }


    public String getCodigoDeSeguranca() {
        return codigoDeSeguranca;
    }

    public void setCodigoDeSeguranca(String codigoDeSeguranca) {
        this.codigoDeSeguranca = codigoDeSeguranca;
    }

    public Boolean getIgnorarValidacao() {
        return ignorarValidacao;
    }

    public void setIgnorarValidacao(Boolean ignorarValidacao) {
        this.ignorarValidacao = ignorarValidacao;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getTitularCPF() {
        return titularCPF;
    }

    public void setTitularCPF(String titularCPF) {
        this.titularCPF = titularCPF;
    }

    public String getTitularNome() {
        return titularNome;
    }

    public void setTitularNome(String titularNome) {
        this.titularNome = titularNome;
    }

    public String getValidade() {
        return validade;
    }

    public void setValidade(String validade) {
        this.validade = validade;
    }

    public Integer getParcelas() {
        return parcelas;
    }

    public void setParcelas(Integer parcelas) {
        this.parcelas = parcelas;
    }

    public Integer getFinanciamentoPagamento() {
        return financiamentoPagamento;
    }

    public void setFinanciamentoPagamento(Integer financiamentoPagamento) {
        this.financiamentoPagamento = financiamentoPagamento;
    }

    @Override
    public String toString() {
        return "CartaoDeCredito{" +
                "id=" + id +
                ", autorizacao='" + autorizacao + '\'' +
                ", bandeira=" + bandeira +
                ", codigoDeSeguranca='" + codigoDeSeguranca + '\'' +
                ", ignorarValidacao=" + ignorarValidacao +
                ", numero='" + numero + '\'' +
                ", titularCPF='" + titularCPF + '\'' +
                ", titularNome='" + titularNome + '\'' +
                ", validade='" + validade + '\'' +
                ", parcelas=" + parcelas +
                ", financiamentoPagamento=" + financiamentoPagamento +
                '}';
    }
}

