package com.confApi.db.confManager.recebimento;

import com.confApi.db.confManager.bandeira.Bandeira;
import com.confApi.db.confManager.carro.CarroReserva;
import com.confApi.db.confManager.formaPagamento.FormaPagamento;
import com.confApi.db.confManager.gatewayCartao.GatewayCartao;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.reservaHotel.dto.ReservaHotel;
import com.confApi.db.confManager.reservaPacote.ReservaPacote;
import com.confApi.db.confManager.seguro.reserva.SeguroReserva;
import com.confApi.endPoints.recebimento.RecebimentoResponse;
import com.confApi.endPoints.reservaPacote.ReservaPacoteResponse;
import com.confApi.hub.aereo.CartaoModel;
import com.confApi.hub.aereo.ReservaAereoModel;
import com.confApi.model.RecebimentoModel;
import com.confApi.seguros.dto.SeguroCompraModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Recebimento implements Serializable {


    private Integer codgRecebimento;
    private Double valrRecebimento;
    private String numrCartao;
    private String validadeCartao;
    private String codgSegCartao;
    private String titularCartao;
    private Integer qtdeParcela;
    private Double valrPrimeiraParcela;
    private Double valrDemaisParcela;
    private String codgAutCartao;
    private String codgTransacao;
    private String orderGatewayCartao;
    private Integer status;
    private Double valrEntrada;
    private Date dataRecebimento;
    private Bandeira codgBandeira;
    private GatewayCartao codgGatewayCartao;
    private FormaPagamento codgFormaPagto;
    private Double valrCancelado;
    private ReservaAereo codgReservaAereo;
    private String link;
    private ReservaHotel codgReservaHotel;
    private String assinaturaEletronica;
    private String mensagem;
    private String qrcodePix;
    private String copiacolaPix;
    private ReservaPacote codgReservaPacote;
    private SeguroReserva codgReservaSeguro;
    private CarroReserva codgReservaCarro;

    public Recebimento(ReservaAereoModel reservaAereoModel) {
        if (reservaAereoModel == null || reservaAereoModel.getRecebimento() == null) {
            return;
        }

        com.confApi.hub.aereo.RecebimentoModel recebimentoModel = reservaAereoModel.getRecebimento();

        this.codgRecebimento = recebimentoModel.getCodgRecebimento();
        this.valrRecebimento = reservaAereoModel.getValorTotalReserva();
        this.valrEntrada = reservaAereoModel.getValorTotalReserva();

        if (recebimentoModel.getCartaoSelecionado() != null) {
            CartaoModel cartao = recebimentoModel.getCartaoSelecionado();

            this.numrCartao = cartao.getNumeroCartao();
            this.validadeCartao = cartao.getValidadeCartao();
            this.codgSegCartao = cartao.getCodgSegurancaCartao();
            this.titularCartao = cartao.getTitularBandeira();

            if (cartao.getParcelaSelecionada() != null) {
                this.qtdeParcela = cartao.getParcelaSelecionada().getNumeroDaParcela();
                this.valrPrimeiraParcela = cartao.getParcelaSelecionada().getValorPrimeiraParcela();
                this.valrDemaisParcela = cartao.getParcelaSelecionada().getValorDemaisParcelas();
                this.valrEntrada = cartao.getParcelaSelecionada().getValorPrimeiraParcela();
            }

            this.codgAutCartao = cartao.getCodgAutorizacao();
            this.codgTransacao = cartao.getCodgTransacao();
            this.orderGatewayCartao = cartao.getCodgTransacao();

            this.codgBandeira = new Bandeira(1);
        }

        this.assinaturaEletronica = recebimentoModel.getAssinatura();
        this.status = recebimentoModel.getStatusRecebimento() != null
                ? recebimentoModel.getStatusRecebimento()
                : 1;

        this.dataRecebimento = recebimentoModel.getDataRecebimento() != null
                ? recebimentoModel.getDataRecebimento()
                : new Date();

        this.codgGatewayCartao = null;

        if (recebimentoModel.getFormaDePagamento() != null
                && recebimentoModel.getFormaDePagamento().getCodgFormaPagto() != null) {
            this.codgFormaPagto = new FormaPagamento(
                    recebimentoModel.getFormaDePagamento().getCodgFormaPagto()
            );
        } else if (recebimentoModel.getCodgFormaPagamento() != null) {
            this.codgFormaPagto = new FormaPagamento(
                    recebimentoModel.getCodgFormaPagamento()
            );
        }

        this.valrCancelado = 0.0;

        if (reservaAereoModel.getCodgReservaAereoDB() != null) {
            this.codgReservaAereo = new ReservaAereo(
                    reservaAereoModel.getCodgReservaAereoDB().intValue()
            );
        }

        this.link = recebimentoModel.getLink();

        this.codgReservaHotel = null;
        this.codgReservaPacote = null;
        this.codgReservaSeguro = null;

        this.mensagem = null;
        this.qrcodePix = null;
        this.copiacolaPix = null;
    }

    public Recebimento(RecebimentoModel recebimentoModel) {
        this.codgRecebimento = recebimentoModel.getCodgRecebimento() != null ? recebimentoModel.getCodgRecebimento() : null;
        this.valrRecebimento = recebimentoModel.getValorPagamento();
        if(recebimentoModel.getCartaoSelecionado() != null){
            this.numrCartao = recebimentoModel.getCartaoSelecionado().getNumeroCartao();
            this.validadeCartao = recebimentoModel.getCartaoSelecionado().getValidadeCartao();
            this.codgSegCartao = recebimentoModel.getCartaoSelecionado().getCodgSegurancaCartao();
            this.titularCartao = recebimentoModel.getCartaoSelecionado().getTitularBandeira();
            this.qtdeParcela = Integer.parseInt(recebimentoModel.getCartaoSelecionado().getQuantidadeParcelas());
            this.codgBandeira = new Bandeira(Integer.parseInt(recebimentoModel.getCartaoSelecionado().getCodgBandeira()));
        }
        this.valrPrimeiraParcela = recebimentoModel.getValorEntrada();
        this.valrDemaisParcela = recebimentoModel.getValorEntrada();
        this.codgAutCartao = null;
        this.codgTransacao = null;
        this.orderGatewayCartao = null;
        this.status = null;
        this.valrEntrada = recebimentoModel.getValorEntrada();
        this.dataRecebimento = new Date();
        this.codgGatewayCartao = null;
        this.codgFormaPagto = recebimentoModel.getFormaDePagamento();
        this.valrCancelado = null;
        this.codgReservaAereo = null;
        this.link = null;
        this.codgReservaHotel = null;
        this.assinaturaEletronica = null;
        this.mensagem = null;
        this.qrcodePix = null;
        this.copiacolaPix = null;
        this.codgReservaPacote = null;
        this.codgReservaSeguro = null;
        this.codgReservaCarro = null;
    }

    public Recebimento(com.confApi.hub.aereo.RecebimentoModel recebimentoModel) {
        this.codgRecebimento = recebimentoModel.getCodgRecebimento() != null ? recebimentoModel.getCodgRecebimento() : null;
        this.valrRecebimento = recebimentoModel.getValorPagamento();
        if(recebimentoModel.getCartaoSelecionado() != null){
            this.numrCartao = recebimentoModel.getCartaoSelecionado().getNumeroCartao();
            this.validadeCartao = recebimentoModel.getCartaoSelecionado().getValidadeCartao();
            this.codgSegCartao = recebimentoModel.getCartaoSelecionado().getCodgSegurancaCartao();
            this.titularCartao = recebimentoModel.getCartaoSelecionado().getTitularBandeira();
            this.qtdeParcela = recebimentoModel.getCartaoSelecionado().getQuantidadeParcelas() != null ?
                Integer.parseInt(recebimentoModel.getCartaoSelecionado().getQuantidadeParcelas()) : null;
            this.codgBandeira = recebimentoModel.getCartaoSelecionado().getCodgBandeira() != null ?
                    new Bandeira(Integer.parseInt(recebimentoModel.getCartaoSelecionado().getCodgBandeira())) : null;
        }
        this.valrPrimeiraParcela = recebimentoModel.getValorEntrada();
        this.valrDemaisParcela = recebimentoModel.getValorEntrada();
        this.codgAutCartao = null;
        this.codgTransacao = null;
        this.orderGatewayCartao = null;
        this.status = null;
        this.valrEntrada = recebimentoModel.getValorEntrada();
        this.dataRecebimento = new Date();
        this.codgGatewayCartao = null;
        this.codgFormaPagto = recebimentoModel.getFormaDePagamento();
        this.valrCancelado = null;
        this.codgReservaAereo = null;
        this.link = null;
        this.codgReservaHotel = null;
        this.assinaturaEletronica = null;
        this.mensagem = null;
        this.qrcodePix = null;
        this.copiacolaPix = null;
        this.codgReservaPacote = null;
        this.codgReservaSeguro = null;
    }

    public Recebimento() {
    }

    public Recebimento(RecebimentoResponse recebimentoResponse, ReservaPacoteResponse reservaPacoteResponse) {
        this.codgRecebimento = recebimentoResponse.getCodgRecebimento();
        this.valrRecebimento = recebimentoResponse.getValorPagamento();
        this.numrCartao = recebimentoResponse.getCartaoSelecionado().getNumeroCartao();
        this.validadeCartao = recebimentoResponse.getCartaoSelecionado().getValidadeCartao();
        this.codgSegCartao = recebimentoResponse.getCartaoSelecionado().getCodgSegurancaCartao();
        this.titularCartao = recebimentoResponse.getCartaoSelecionado().getTitularBandeira();
        this.qtdeParcela = Integer.parseInt(recebimentoResponse.getCartaoSelecionado().getQuantidadeParcelas());
        this.valrPrimeiraParcela = recebimentoResponse.getValorEntrada();
        this.valrDemaisParcela = recebimentoResponse.getValorEntrada();
        this.codgAutCartao = recebimentoResponse.getCartaoSelecionado().getCodgAutorizacao();
        this.codgTransacao = recebimentoResponse.getCartaoSelecionado().getCodgTransacao();
        this.orderGatewayCartao = null;
        this.status = recebimentoResponse.getStatusRecebimento();
        this.valrEntrada = recebimentoResponse.getValorEntrada();
        this.dataRecebimento = recebimentoResponse.getDataRecebimento();
        this.codgBandeira = new Bandeira(recebimentoResponse.getCartaoSelecionado());
        this.codgGatewayCartao = null;
        this.codgFormaPagto = new FormaPagamento(recebimentoResponse.getCodgFormaPagamento());
        this.valrCancelado = 0.0;
        this.codgReservaAereo = null;
        this.link = recebimentoResponse.getLink();
        this.codgReservaHotel = null;
        this.assinaturaEletronica = recebimentoResponse.getAssinatura();
        this.mensagem = null;
        this.qrcodePix = null;
        this.copiacolaPix = null;
        this.codgReservaPacote = new ReservaPacote(reservaPacoteResponse);
        this.codgReservaSeguro = null;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getAssinaturaEletronica() {
        return assinaturaEletronica;
    }

    public void setAssinaturaEletronica(String assinaturaEletronica) {
        this.assinaturaEletronica = assinaturaEletronica;
    }

    public ReservaAereo getCodgReservaAereo() {
        return codgReservaAereo;
    }

    public void setCodgReservaAereo(ReservaAereo codgReservaAereo) {
        this.codgReservaAereo = codgReservaAereo;
    }

    public GatewayCartao getCodgGatewayCartao() {
        return codgGatewayCartao;
    }

    public void setCodgGatewayCartao(GatewayCartao codgGatewayCartao) {
        this.codgGatewayCartao = codgGatewayCartao;
    }

    public FormaPagamento getCodgFormaPagto() {
        return codgFormaPagto;
    }

    public void setCodgFormaPagto(FormaPagamento codgFormaPagto) {
        this.codgFormaPagto = codgFormaPagto;
    }

    public Bandeira getCodgBandeira() {
        return codgBandeira;
    }

    public void setCodgBandeira(Bandeira codgBandeira) {
        this.codgBandeira = codgBandeira;
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

    public Double getValrRecebimento() {
        return valrRecebimento;
    }

    public void setValrRecebimento(Double valrRecebimento) {
        this.valrRecebimento = valrRecebimento;
    }

    public String getNumrCartao() {
        return numrCartao;
    }

    public void setNumrCartao(String numrCartao) {
        this.numrCartao = numrCartao;
    }

    public String getValidadeCartao() {
        return validadeCartao;
    }

    public void setValidadeCartao(String validadeCartao) {
        this.validadeCartao = validadeCartao;
    }

    public String getCodgSegCartao() {
        return codgSegCartao;
    }

    public void setCodgSegCartao(String codgSegCartao) {
        this.codgSegCartao = codgSegCartao;
    }

    public String getTitularCartao() {
        return titularCartao;
    }

    public void setTitularCartao(String titularCartao) {
        this.titularCartao = titularCartao;
    }

    public Integer getQtdeParcela() {
        return qtdeParcela;
    }

    public void setQtdeParcela(Integer qtdeParcela) {
        this.qtdeParcela = qtdeParcela;
    }

    public Double getValrPrimeiraParcela() {
        return valrPrimeiraParcela;
    }

    public void setValrPrimeiraParcela(Double valrPrimeiraParcela) {
        this.valrPrimeiraParcela = valrPrimeiraParcela;
    }

    public Double getValrDemaisParcela() {
        return valrDemaisParcela;
    }

    public void setValrDemaisParcela(Double valrDemaisParcela) {
        this.valrDemaisParcela = valrDemaisParcela;
    }

    public String getCodgAutCartao() {
        return codgAutCartao;
    }

    public void setCodgAutCartao(String codgAutCartao) {
        this.codgAutCartao = codgAutCartao;
    }

    public String getCodgTransacao() {
        return codgTransacao;
    }

    public void setCodgTransacao(String codgTransacao) {
        this.codgTransacao = codgTransacao;
    }

    public String getOrderGatewayCartao() {
        return orderGatewayCartao;
    }

    public void setOrderGatewayCartao(String orderGatewayCartao) {
        this.orderGatewayCartao = orderGatewayCartao;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Double getValrEntrada() {
        return valrEntrada;
    }

    public void setValrEntrada(Double valrEntrada) {
        this.valrEntrada = valrEntrada;
    }

    public Double getValrCancelado() {
        return valrCancelado;
    }

    public void setValrCancelado(Double valrCancelado) {
        this.valrCancelado = valrCancelado;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public ReservaHotel getCodgReservaHotel() {
        return codgReservaHotel;
    }

    public void setCodgReservaHotel(ReservaHotel codgReservaHotel) {
        this.codgReservaHotel = codgReservaHotel;
    }

    public String getQrcodePix() {
        return qrcodePix;
    }

    public void setQrcodePix(String qrcodePix) {
        this.qrcodePix = qrcodePix;
    }

    public String getCopiacolaPix() {
        return copiacolaPix;
    }

    public void setCopiacolaPix(String copiacolaPix) {
        this.copiacolaPix = copiacolaPix;
    }

    public ReservaPacote getCodgReservaPacote() {
        return codgReservaPacote;
    }

    public void setCodgReservaPacote(ReservaPacote codgReservaPacote) {
        this.codgReservaPacote = codgReservaPacote;
    }

    public SeguroReserva getCodgReservaSeguro() {
        return codgReservaSeguro;
    }

    public void setCodgReservaSeguro(SeguroReserva codgReservaSeguro) {
        this.codgReservaSeguro = codgReservaSeguro;
    }

    public CarroReserva getCodgReservaCarro() {
        return codgReservaCarro;
    }

    public void setCodgReservaCarro(CarroReserva codgReservaCarro) {
        this.codgReservaCarro = codgReservaCarro;
    }

    @Override
    public String toString() {
        return "Recebimento{" +
                "codgRecebimento=" + codgRecebimento +
                ", valrRecebimento=" + valrRecebimento +
                ", qtdeParcela=" + qtdeParcela +
                ", valrPrimeiraParcela=" + valrPrimeiraParcela +
                ", valrDemaisParcela=" + valrDemaisParcela +
                ", status=" + status +
                ", valrEntrada=" + valrEntrada +
                ", dataRecebimento=" + dataRecebimento +
                ", codgFormaPagto=" + codgFormaPagto +
                ", valrCancelado=" + valrCancelado +
                ", possuiCartao=" + (numrCartao != null && !numrCartao.isBlank()) +
                ", possuiPix=" + ((qrcodePix != null && !qrcodePix.isBlank())
                        || (copiacolaPix != null && !copiacolaPix.isBlank())) +
                ", possuiLink=" + (link != null && !link.isBlank()) +
                '}';
    }
}
