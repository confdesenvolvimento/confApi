package com.confApi.db.confManager.recebimento;

import com.confApi.db.confManager.bandeira.Bandeira;
import com.confApi.db.confManager.formaPagamento.FormaPagamento;
import com.confApi.db.confManager.gatewayCartao.GatewayCartao;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.reservaHotel.dto.ReservaHotel;
import com.confApi.db.confManager.reservaPacote.ReservaPacote;
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



    @Override
    public String toString() {
        return "Recebimento{" + "codgRecebimento=" + codgRecebimento + ", valrRecebimento=" + valrRecebimento + ", numrCartao=" + numrCartao + ", validadeCartao=" + validadeCartao + ", codgSegCartao=" + codgSegCartao + ", titularCartao=" + titularCartao + ", qtdeParcela=" + qtdeParcela + ", valrPrimeiraParcela=" + valrPrimeiraParcela + ", valrDemaisParcela=" + valrDemaisParcela + ", codgAutCartao=" + codgAutCartao + ", codgTransacao=" + codgTransacao + ", orderGatewayCartao=" + orderGatewayCartao + ", status=" + status + ", valrEntrada=" + valrEntrada + ", dataRecebimento=" + dataRecebimento + ", codgBandeira=" + codgBandeira + ", codgGatewayCartao=" + codgGatewayCartao + ", codgFormaPagto=" + codgFormaPagto + ", valrCancelado=" + valrCancelado + ", codgReservaAereo=" + codgReservaAereo + ", link=" + link + ", codgReservaHotel=" + codgReservaHotel + ", assinaturaEletronica=" + assinaturaEletronica + ", mensagem=" + mensagem + ", qrcodePix=" + qrcodePix + ", copiacolaPix=" + copiacolaPix + '}';
    }


}

