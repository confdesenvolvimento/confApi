package com.confApi.hub.aereo;

import java.io.Serializable;

public class PagamentoModel implements Serializable {

    private Long codgFormaPagamento;
    private Double valorEntrada = 0.0;
    private Double valorPagamento = 0.0;
    private CartaoModel cartao;

    public Long getCodgFormaPagamento() {
        return codgFormaPagamento;
    }

    public void setCodgFormaPagamento(Long codgFormaPagamento) {
        this.codgFormaPagamento = codgFormaPagamento;
    }

    public Double getValorEntrada() {
        return valorEntrada;
    }

    public void setValorEntrada(Double valorEntrada) {
        this.valorEntrada = valorEntrada;
    }

    public Double getValorPagamento() {
        return valorPagamento;
    }

    public void setValorPagamento(Double valorPagamento) {
        this.valorPagamento = valorPagamento;
    }

    public CartaoModel getCartao() {
        return cartao;
    }

    public void setCartao(CartaoModel cartao) {
        this.cartao = cartao;
    }



}
