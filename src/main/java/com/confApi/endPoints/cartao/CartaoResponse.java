package com.confApi.endPoints.cartao;

import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.endPoints.parcelaCartao.ParcelaCartaoResponse;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CartaoResponse {
    private String codgBandeira;
    private String siglaBandeira;
    private String nomeBandeira;
    private String titularBandeira;
    private String numeroCartao;
    private String validadeCartao;
    private String codgSegurancaCartao;
    private String quantidadeParcelas;
    private Double valor=0.0;
    private String codgAutorizacao;
    private String codgTransacao;
    private List<ParcelaCartaoResponse> parcelasCartao = new ArrayList<>();
    private ParcelaCartaoResponse parcelaSelecionada;

    public CartaoResponse(Recebimento recebimento) {
        this.codgBandeira = recebimento.getCodgBandeira().getCodgBandeira().toString();
        this.siglaBandeira = recebimento.getCodgBandeira().getSiglaBandeira();
        this.nomeBandeira = recebimento.getCodgBandeira().getNomeBandeira();
        this.titularBandeira = recebimento.getTitularCartao();
        this.numeroCartao = recebimento.getNumrCartao();
        this.validadeCartao = recebimento.getValidadeCartao();
        this.codgSegurancaCartao = recebimento.getCodgSegCartao();
        this.quantidadeParcelas = recebimento.getQtdeParcela().toString();
        this.valor = recebimento.getValrRecebimento();
        this.codgAutorizacao = recebimento.getCodgAutCartao();
        this.codgTransacao = recebimento.getCodgTransacao();
        this.parcelasCartao = null;
        this.parcelaSelecionada = null;
    }
}
