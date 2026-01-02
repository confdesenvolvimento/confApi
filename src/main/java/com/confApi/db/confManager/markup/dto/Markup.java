package com.confApi.db.confManager.markup.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class Markup implements Serializable {

    private Integer codgProduto;
    private Integer codgAgencia;
    private Double valorMarkup;

    public Integer getCodgProduto() { return codgProduto; }
    public void setCodgProduto(Integer codgProduto) { this.codgProduto = codgProduto; }

    public Integer getCodgAgencia() { return codgAgencia; }
    public void setCodgAgencia(Integer codgAgencia) { this.codgAgencia = codgAgencia; }

    public Double getValorMarkup() { return valorMarkup; }
    public void setValorMarkup(Double valorMarkup) { this.valorMarkup = valorMarkup; }
}