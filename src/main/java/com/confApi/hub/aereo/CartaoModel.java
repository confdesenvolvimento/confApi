package com.confApi.hub.aereo;

import com.confApi.endPoints.cartao.CartaoResponse;
import com.confApi.endPoints.parcelaCartao.ParcelaCartaoResponse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CartaoModel implements Serializable {
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
    private List<ParcelaCartaoModel> parcelasCartao = new ArrayList<>();
    private ParcelaCartaoModel parcelaSelecionada;

    public CartaoModel() {
    }

    public CartaoModel(CartaoResponse cartaoResponse) {
        this.codgBandeira = cartaoResponse.getCodgBandeira();
        this.siglaBandeira = cartaoResponse.getSiglaBandeira();
        this.nomeBandeira = cartaoResponse.getNomeBandeira();
        this.titularBandeira = cartaoResponse.getTitularBandeira();
        this.numeroCartao = cartaoResponse.getNumeroCartao();
        this.validadeCartao = cartaoResponse.getValidadeCartao();
        this.codgSegurancaCartao = cartaoResponse.getCodgSegurancaCartao();
        this.quantidadeParcelas = cartaoResponse.getQuantidadeParcelas();
        this.valor = cartaoResponse.getValor();
        this.codgAutorizacao = cartaoResponse.getCodgAutorizacao();
        this.codgTransacao = cartaoResponse.getCodgTransacao();
        this.parcelasCartao = new ArrayList<>();
        for(ParcelaCartaoResponse parcelaCartaoResponse : cartaoResponse.getParcelasCartao()){
            parcelasCartao.add(new ParcelaCartaoModel(parcelaCartaoResponse));
        }
        this.parcelaSelecionada = new ParcelaCartaoModel(cartaoResponse.getParcelaSelecionada());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.codgBandeira);
        hash = 97 * hash + Objects.hashCode(this.siglaBandeira);
        hash = 97 * hash + Objects.hashCode(this.nomeBandeira);
        hash = 97 * hash + Objects.hashCode(this.titularBandeira);
        hash = 97 * hash + Objects.hashCode(this.numeroCartao);
        hash = 97 * hash + Objects.hashCode(this.validadeCartao);
        hash = 97 * hash + Objects.hashCode(this.codgSegurancaCartao);
        hash = 97 * hash + Objects.hashCode(this.quantidadeParcelas);
        hash = 97 * hash + Objects.hashCode(this.valor);
        hash = 97 * hash + Objects.hashCode(this.codgAutorizacao);
        hash = 97 * hash + Objects.hashCode(this.codgTransacao);
        hash = 97 * hash + Objects.hashCode(this.parcelasCartao);
        hash = 97 * hash + Objects.hashCode(this.parcelaSelecionada);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CartaoModel other = (CartaoModel) obj;
        if (!Objects.equals(this.codgBandeira, other.codgBandeira)) {
            return false;
        }
        if (!Objects.equals(this.siglaBandeira, other.siglaBandeira)) {
            return false;
        }
        if (!Objects.equals(this.nomeBandeira, other.nomeBandeira)) {
            return false;
        }
        if (!Objects.equals(this.titularBandeira, other.titularBandeira)) {
            return false;
        }
        if (!Objects.equals(this.numeroCartao, other.numeroCartao)) {
            return false;
        }
        if (!Objects.equals(this.validadeCartao, other.validadeCartao)) {
            return false;
        }
        if (!Objects.equals(this.codgSegurancaCartao, other.codgSegurancaCartao)) {
            return false;
        }
        if (!Objects.equals(this.quantidadeParcelas, other.quantidadeParcelas)) {
            return false;
        }
        if (!Objects.equals(this.codgAutorizacao, other.codgAutorizacao)) {
            return false;
        }
        if (!Objects.equals(this.codgTransacao, other.codgTransacao)) {
            return false;
        }
        if (!Objects.equals(this.valor, other.valor)) {
            return false;
        }
        if (!Objects.equals(this.parcelasCartao, other.parcelasCartao)) {
            return false;
        }
        return Objects.equals(this.parcelaSelecionada, other.parcelaSelecionada);
    }


    public ParcelaCartaoModel getParcelaSelecionada() {
        return parcelaSelecionada;
    }

    public String getSiglaBandeira() {
        return siglaBandeira;
    }

    public void setSiglaBandeira(String siglaBandeira) {
        this.siglaBandeira = siglaBandeira;
    }


    public void setParcelaSelecionada(ParcelaCartaoModel parcelaSelecionada) {
        this.parcelaSelecionada = parcelaSelecionada;
    }

    public String getCodgTransacao() {
        return codgTransacao;
    }

    public void setCodgTransacao(String codgTransacao) {
        this.codgTransacao = codgTransacao;
    }








    public String getCodgAutorizacao() {
        return codgAutorizacao;
    }

    public void setCodgAutorizacao(String codgAutorizacao) {
        this.codgAutorizacao = codgAutorizacao;
    }



    public String getCodgBandeira() {
        return codgBandeira;
    }

    public void setCodgBandeira(String codgBandeira) {
        this.codgBandeira = codgBandeira;
    }

    public String getNomeBandeira() {
        return nomeBandeira;
    }

    public void setNomeBandeira(String nomeBandeira) {
        this.nomeBandeira = nomeBandeira;
    }

    public String getTitularBandeira() {
        return titularBandeira;
    }

    public void setTitularBandeira(String titularBandeira) {
        this.titularBandeira = titularBandeira;
    }

    public String getNumeroCartao() {
        return numeroCartao;
    }

    public void setNumeroCartao(String numeroCartao) {
        this.numeroCartao = numeroCartao;
    }

    public String getValidadeCartao() {
        return validadeCartao;
    }

    public void setValidadeCartao(String validadeCartao) {
        this.validadeCartao = validadeCartao;
    }

    public String getCodgSegurancaCartao() {
        return codgSegurancaCartao;
    }

    public void setCodgSegurancaCartao(String codgSegurancaCartao) {
        this.codgSegurancaCartao = codgSegurancaCartao;
    }

    public String getQuantidadeParcelas() {
        return quantidadeParcelas;
    }

    public void setQuantidadeParcelas(String quantidadeParcelas) {
        this.quantidadeParcelas = quantidadeParcelas;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public List<ParcelaCartaoModel> getParcelasCartao() {
        return parcelasCartao;
    }

    public void setParcelasCartao(List<ParcelaCartaoModel> parcelasCartao) {
        this.parcelasCartao = parcelasCartao;
    }



}
