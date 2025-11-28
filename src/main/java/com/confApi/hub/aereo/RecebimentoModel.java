package com.confApi.hub.aereo;

import com.confApi.endPoints.recebimento.RecebimentoResponse;

import java.io.Serializable;
import java.util.Date;

public class RecebimentoModel implements Serializable {

    private Integer codgFormaPagamento;
    private String nomeFormaPagamento;
    private Double valorEntrada = 0.0;
    private Double valorPagamento = 0.0;
    private FormaPagamentoModel formaDePagamento;
    private CartaoModel cartaoSelecionado;
    private Integer statusRecebimento;
    private Date dataRecebimento;
    private Integer codgCodgRecebimento;
    private String assinatura;
    private String link;

    public RecebimentoModel() {
    }

    public RecebimentoModel(RecebimentoResponse recebimentoResponse) {
        this.codgFormaPagamento = recebimentoResponse.getCodgFormaPagamento();
        this.nomeFormaPagamento = recebimentoResponse.getNomeFormaPagamento();
        this.valorEntrada = recebimentoResponse.getValorEntrada();
        this.valorPagamento = recebimentoResponse.getValorPagamento();
        this.formaDePagamento = new FormaPagamentoModel(recebimentoResponse.getFormaDePagamento());
        this.cartaoSelecionado = new CartaoModel(recebimentoResponse.getCartaoSelecionado());
        this.statusRecebimento = recebimentoResponse.getStatusRecebimento();
        this.dataRecebimento = recebimentoResponse.getDataRecebimento();
        this.codgCodgRecebimento = recebimentoResponse.getCodgCodgRecebimento();
        this.assinatura = recebimentoResponse.getAssinatura();
        this.link = recebimentoResponse.getLink();
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }




    public String getAssinatura() {
        return assinatura;
    }

    public void setAssinatura(String assinatura) {
        this.assinatura = assinatura;
    }



    public Integer getCodgCodgRecebimento() {
        return codgCodgRecebimento;
    }

    public void setCodgCodgRecebimento(Integer codgCodgRecebimento) {
        this.codgCodgRecebimento = codgCodgRecebimento;
    }



    public Date getDataRecebimento() {
        return dataRecebimento;
    }

    public void setDataRecebimento(Date dataRecebimento) {
        this.dataRecebimento = dataRecebimento;
    }



    public Integer getStatusRecebimento() {
        return statusRecebimento;
    }

    public void setStatusRecebimento(Integer statusRecebimento) {
        this.statusRecebimento = statusRecebimento;
    }





    public CartaoModel getCartaoSelecionado() {
        return cartaoSelecionado;
    }

    public void setCartaoSelecionado(CartaoModel cartaoSelecionado) {
        this.cartaoSelecionado = cartaoSelecionado;
    }




    public FormaPagamentoModel getFormaDePagamento() {
        return formaDePagamento;
    }

    public void setFormaDePagamento(FormaPagamentoModel formaDePagamento) {
        this.formaDePagamento = formaDePagamento;
    }



    public Integer getCodgFormaPagamento() {
        return codgFormaPagamento;
    }

    public void setCodgFormaPagamento(Integer codgFormaPagamento) {
        this.codgFormaPagamento = codgFormaPagamento;
    }

    public String getNomeFormaPagamento() {
        return nomeFormaPagamento;
    }

    public void setNomeFormaPagamento(String nomeFormaPagamento) {
        this.nomeFormaPagamento = nomeFormaPagamento;
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

}
