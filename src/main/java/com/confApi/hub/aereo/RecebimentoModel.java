package com.confApi.hub.aereo;

import com.confApi.db.confManager.recebimento.Recebimento;
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
    private Integer codgRecebimento;
    private String assinatura;
    private String link;

    public RecebimentoModel() {
    }

    public RecebimentoModel(Integer codgFormaPagamento, String nomeFormaPagamento,
                            Double valorEntrada, Double valorPagamento,
                            FormaPagamentoModel formaDePagamento, CartaoModel cartaoSelecionado,
                            Integer statusRecebimento, Date dataRecebimento, Integer codgRecebimento,
                            String assinatura, String link) {
        this.codgFormaPagamento = codgFormaPagamento;
        this.nomeFormaPagamento = nomeFormaPagamento;
        this.valorEntrada = valorEntrada;
        this.valorPagamento = valorPagamento;
        this.formaDePagamento = formaDePagamento;
        this.cartaoSelecionado = cartaoSelecionado;
        this.statusRecebimento = statusRecebimento;
        this.dataRecebimento = dataRecebimento;
        this.codgRecebimento = codgRecebimento;
        this.assinatura = assinatura;
        this.link = link;
    }

    public RecebimentoModel(Recebimento recebimento) {
        this.codgFormaPagamento = recebimento.getCodgFormaPagto().getCodgFormaPagto();
        this.nomeFormaPagamento = recebimento.getCodgFormaPagto().getNomeFormaPagto();
        this.valorEntrada = recebimento.getValrEntrada();
        this.valorPagamento = recebimento.getValrRecebimento();
        this.formaDePagamento = new FormaPagamentoModel(recebimento.getCodgFormaPagto());
        this.cartaoSelecionado = new CartaoModel(recebimento);
        this.statusRecebimento = recebimento.getStatus();
        this.dataRecebimento = recebimento.getDataRecebimento();
        this.codgRecebimento = recebimento.getCodgRecebimento();
        this.assinatura = recebimento.getAssinaturaEletronica();
        this.link = recebimento.getLink();
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
        this.codgRecebimento = recebimentoResponse.getCodgRecebimento();
        this.assinatura = recebimentoResponse.getAssinatura();
        this.link = recebimentoResponse.getLink();
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

    public FormaPagamentoModel getFormaDePagamento() {
        return formaDePagamento;
    }

    public void setFormaDePagamento(FormaPagamentoModel formaDePagamento) {
        this.formaDePagamento = formaDePagamento;
    }

    public CartaoModel getCartaoSelecionado() {
        return cartaoSelecionado;
    }

    public void setCartaoSelecionado(CartaoModel cartaoSelecionado) {
        this.cartaoSelecionado = cartaoSelecionado;
    }

    public Integer getStatusRecebimento() {
        return statusRecebimento;
    }

    public void setStatusRecebimento(Integer statusRecebimento) {
        this.statusRecebimento = statusRecebimento;
    }

    public Date getDataRecebimento() {
        return dataRecebimento;
    }

    public void setDataRecebimento(Date dataRecebimento) {
        this.dataRecebimento = dataRecebimento;
    }

    public Integer getCodgRecebimento() {
        return codgRecebimento;
    }

    public void setCodgRecebimento(Integer codgRecebimento) {
        this.codgRecebimento = codgRecebimento;
    }

    public String getAssinatura() {
        return assinatura;
    }

    public void setAssinatura(String assinatura) {
        this.assinatura = assinatura;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    @Override
    public String toString() {
        return "RecebimentoModel{" +
                "codgFormaPagamento=" + codgFormaPagamento +
                ", nomeFormaPagamento='" + nomeFormaPagamento + '\'' +
                ", valorEntrada=" + valorEntrada +
                ", valorPagamento=" + valorPagamento +
                ", formaDePagamento=" + formaDePagamento +
                ", cartaoSelecionado=" + cartaoSelecionado +
                ", statusRecebimento=" + statusRecebimento +
                ", dataRecebimento=" + dataRecebimento +
                ", codgRecebimento=" + codgRecebimento +
                ", assinatura='" + assinatura + '\'' +
                ", link='" + link + '\'' +
                '}';
    }
}
