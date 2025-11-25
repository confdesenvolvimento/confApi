package com.confApi.db.confManager.hotelQuartoValor;

import com.confApi.db.confManager.hotelHospede.HotelHospede;
import com.confApi.db.confManager.reservaHotel.dto.ReservaHotel;

import java.io.Serializable;
import java.util.List;

public class HotelQuartoValor implements Serializable {

    private Integer codgHotelQuartoHotel;
    private ReservaHotel codgReservaHotel;
    private String siglaTipoQuarto;
    private String nomeTipoQuarto;
    private String nomeQuarto;
    private String descricaoRegime;
    private String codgFornecedorSistema;
    private Double valorDiariaMarkup=0.0;
    private Double valorDiariaNet=0.0;
    private Double valorDiariaTotalMarkup=0.0;
    private Double valorDiariaTotalNet=0.0;
    private Double valorTotalEstadiaNet=0.0;
    private Double valorTotalEstadiaMarkup=0.0;
    private Double percTaxaIss=0.0;
    private Double valorTaxaIss=0.0;
    private Double percTaxaServico=0.0;
    private Double valorTaxaServico=0.0;
    private Double percMkpAplicado=0.0;
    private Double valorMkpAplicado=0.0;
    private Double valorTaxasExtras=0.0;
    private List<HotelHospede> hotelHospedeList;

    private Integer qtdAdt=0;
    private Integer qtdChd=0;
    private Integer qtdInf=0;


    public Integer getCodgHotelQuartoHotel() {
        return codgHotelQuartoHotel;
    }

    public void setCodgHotelQuartoHotel(Integer codgHotelQuartoHotel) {
        this.codgHotelQuartoHotel = codgHotelQuartoHotel;
    }

    public ReservaHotel getCodgReservaHotel() {
        return codgReservaHotel;
    }

    public void setCodgReservaHotel(ReservaHotel codgReservaHotel) {
        this.codgReservaHotel = codgReservaHotel;
    }

    public String getSiglaTipoQuarto() {
        return siglaTipoQuarto;
    }

    public void setSiglaTipoQuarto(String siglaTipoQuarto) {
        this.siglaTipoQuarto = siglaTipoQuarto;
    }

    public String getNomeTipoQuarto() {
        return nomeTipoQuarto;
    }

    public void setNomeTipoQuarto(String nomeTipoQuarto) {
        this.nomeTipoQuarto = nomeTipoQuarto;
    }

    public String getNomeQuarto() {
        return nomeQuarto;
    }

    public void setNomeQuarto(String nomeQuarto) {
        this.nomeQuarto = nomeQuarto;
    }

    public String getDescricaoRegime() {
        return descricaoRegime;
    }

    public void setDescricaoRegime(String descricaoRegime) {
        this.descricaoRegime = descricaoRegime;
    }

    public String getCodgFornecedorSistema() {
        return codgFornecedorSistema;
    }

    public void setCodgFornecedorSistema(String codgFornecedorSistema) {
        this.codgFornecedorSistema = codgFornecedorSistema;
    }

    public Double getValorDiariaMarkup() {
        return valorDiariaMarkup;
    }

    public void setValorDiariaMarkup(Double valorDiariaMarkup) {
        this.valorDiariaMarkup = valorDiariaMarkup;
    }

    public Double getValorDiariaNet() {
        return valorDiariaNet;
    }

    public void setValorDiariaNet(Double valorDiariaNet) {
        this.valorDiariaNet = valorDiariaNet;
    }

    public Double getValorDiariaTotalMarkup() {
        return valorDiariaTotalMarkup;
    }

    public void setValorDiariaTotalMarkup(Double valorDiariaTotalMarkup) {
        this.valorDiariaTotalMarkup = valorDiariaTotalMarkup;
    }

    public Double getValorDiariaTotalNet() {
        return valorDiariaTotalNet;
    }

    public void setValorDiariaTotalNet(Double valorDiariaTotalNet) {
        this.valorDiariaTotalNet = valorDiariaTotalNet;
    }

    public Double getValorTotalEstadiaNet() {
        return valorTotalEstadiaNet;
    }

    public void setValorTotalEstadiaNet(Double valorTotalEstadiaNet) {
        this.valorTotalEstadiaNet = valorTotalEstadiaNet;
    }

    public Double getValorTotalEstadiaMarkup() {
        return valorTotalEstadiaMarkup;
    }

    public void setValorTotalEstadiaMarkup(Double valorTotalEstadiaMarkup) {
        this.valorTotalEstadiaMarkup = valorTotalEstadiaMarkup;
    }

    public Double getPercTaxaIss() {
        return percTaxaIss;
    }

    public void setPercTaxaIss(Double percTaxaIss) {
        this.percTaxaIss = percTaxaIss;
    }

    public Double getValorTaxaIss() {
        return valorTaxaIss;
    }

    public void setValorTaxaIss(Double valorTaxaIss) {
        this.valorTaxaIss = valorTaxaIss;
    }

    public Double getPercTaxaServico() {
        return percTaxaServico;
    }

    public void setPercTaxaServico(Double percTaxaServico) {
        this.percTaxaServico = percTaxaServico;
    }

    public Double getValorTaxaServico() {
        return valorTaxaServico;
    }

    public void setValorTaxaServico(Double valorTaxaServico) {
        this.valorTaxaServico = valorTaxaServico;
    }

    public Double getPercMkpAplicado() {
        return percMkpAplicado;
    }

    public void setPercMkpAplicado(Double percMkpAplicado) {
        this.percMkpAplicado = percMkpAplicado;
    }

    public Double getValorMkpAplicado() {
        return valorMkpAplicado;
    }

    public void setValorMkpAplicado(Double valorMkpAplicado) {
        this.valorMkpAplicado = valorMkpAplicado;
    }

    public Double getValorTaxasExtras() {
        return valorTaxasExtras;
    }

    public void setValorTaxasExtras(Double valorTaxasExtras) {
        this.valorTaxasExtras = valorTaxasExtras;
    }

    public List<HotelHospede> getHotelHospedeList() {
        return hotelHospedeList;
    }

    public void setHotelHospedeList(List<HotelHospede> hotelHospedeList) {
        this.hotelHospedeList = hotelHospedeList;
    }

    public Integer getQtdAdt() {
        return qtdAdt;
    }

    public void setQtdAdt(Integer qtdAdt) {
        this.qtdAdt = qtdAdt;
    }

    public Integer getQtdChd() {
        return qtdChd;
    }

    public void setQtdChd(Integer qtdChd) {
        this.qtdChd = qtdChd;
    }

    public Integer getQtdInf() {
        return qtdInf;
    }

    public void setQtdInf(Integer qtdInf) {
        this.qtdInf = qtdInf;
    }


}


