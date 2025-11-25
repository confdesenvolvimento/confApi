package com.confApi.db.confManager.hotel.model;

import com.confApi.db.confManager.hotelQuartoValor.HotelQuartoValor;
import com.confApi.db.confManager.reservaHotel.dto.ReservaHotel;
import com.confApi.hub.hotel.dto.TarifaDiaria;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TarifaHotel implements Serializable {

    private Double valorMarkupAplicado = 0.0;
    private Double percentualMarkupAplicado = 0.0;
    private Double valorTotalEstadiaComMarkup = 0.0;
    private Double valorTotalEstadiaComMarkupBrl = 0.0;
    private Double valorTotalEstadiaNet = 0.0;
    private Double mediaDiaria = 0.0;
    private Double mediaDiariaNet = 0.0;
    private Double totalDiarias = 0.0;
    private Double percentualTaxaIss = 0.0;
    private Double percentualTaxaServico = 0.0;
    private Double percentualTaxaExtra = 0.0;
    private Double valorTaxaIss = 0.0;
    private Double valorTaxaServico = 0.0;
    private String moeda;
    private String disponibilidade;
    private List<TarifaDiaria> tarifasDiaria = new ArrayList<>();

    public TarifaHotel() {
    }



    public TarifaHotel(HotelQuartoValor hotelQuartoValor, ReservaHotel reservaHotel) {
        this.valorMarkupAplicado = hotelQuartoValor.getPercMkpAplicado();
        this.percentualMarkupAplicado = hotelQuartoValor.getPercMkpAplicado();
        this.valorTotalEstadiaComMarkup = hotelQuartoValor.getValorTotalEstadiaMarkup();
        this.valorTotalEstadiaComMarkupBrl = hotelQuartoValor.getValorTotalEstadiaMarkup();
        this.valorTotalEstadiaNet = hotelQuartoValor.getValorTotalEstadiaNet();
        this.mediaDiaria = hotelQuartoValor.getValorDiariaMarkup();
        this.mediaDiariaNet = hotelQuartoValor.getValorDiariaNet();
        this.totalDiarias = hotelQuartoValor.getValorTotalEstadiaMarkup();
        this.percentualTaxaIss = hotelQuartoValor.getPercTaxaIss();
        this.percentualTaxaServico = hotelQuartoValor.getPercTaxaServico();
        this.percentualTaxaExtra = null;
        this.valorTaxaIss = hotelQuartoValor.getValorTaxaIss();
        this.valorTaxaServico = hotelQuartoValor.getValorTaxaServico();
        this.moeda = "BRL";
        this.disponibilidade = null;
//        this.tarifasDiaria =  null;
    }





    public void calcularTotais(TarifaHotel tarifa) {

        if (tarifa != null) {
            valorMarkupAplicado += tarifa.getValorMarkupAplicado();
            percentualMarkupAplicado += tarifa.getPercentualMarkupAplicado();
            valorTotalEstadiaComMarkup += tarifa.getValorTotalEstadiaComMarkup();
            valorTotalEstadiaComMarkupBrl += tarifa.getValorTotalEstadiaComMarkupBrl();
            valorTotalEstadiaNet += tarifa.getValorTotalEstadiaNet();
            mediaDiaria += tarifa.getMediaDiaria();
            mediaDiariaNet += tarifa.getMediaDiariaNet();
            totalDiarias += tarifa.getTotalDiarias();
            percentualTaxaIss += tarifa.getPercentualTaxaIss();
            percentualTaxaServico += tarifa.getPercentualTaxaServico();
            percentualTaxaExtra += tarifa.getPercentualTaxaExtra();
            valorTaxaIss += tarifa.getValorTaxaIss();
            valorTaxaServico += tarifa.getValorTaxaServico();
        }

    }

    public List<TarifaDiaria> getTarifasDiaria() {
        return tarifasDiaria;
    }

    public Double getMediaDiariaNet() {
        return mediaDiariaNet;
    }

    public void setMediaDiariaNet(Double mediaDiariaNet) {
        this.mediaDiariaNet = mediaDiariaNet;
    }

    public Double getTotalDiarias() {
        return totalDiarias;
    }

    public void setTotalDiarias(Double totalDiarias) {
        this.totalDiarias = totalDiarias;
    }

    public void setTarifasDiaria(List<TarifaDiaria> tarifasDiaria) {
        this.tarifasDiaria = tarifasDiaria;
    }

    public Double getMediaDiaria() {
        return mediaDiaria;
    }

    public void setMediaDiaria(Double mediaDiaria) {
        this.mediaDiaria = mediaDiaria;
    }

    public String getDisponibilidade() {
        return disponibilidade;
    }

    public void setDisponibilidade(String disponibilidade) {
        this.disponibilidade = disponibilidade;
    }

    public String getMoeda() {
        return moeda;
    }

    public void setMoeda(String moeda) {
        this.moeda = moeda;
    }

    public Double getValorMarkupAplicado() {
        return valorMarkupAplicado;
    }

    public void setValorMarkupAplicado(Double valorMarkupAplicado) {
        this.valorMarkupAplicado = valorMarkupAplicado;
    }

    public Double getPercentualMarkupAplicado() {
        return percentualMarkupAplicado;
    }

    public void setPercentualMarkupAplicado(Double percentualMarkupAplicado) {
        this.percentualMarkupAplicado = percentualMarkupAplicado;
    }

    public Double getValorTotalEstadiaComMarkup() {
        return valorTotalEstadiaComMarkup;
    }

    public void setValorTotalEstadiaComMarkup(Double valorTotalEstadiaComMarkup) {
        this.valorTotalEstadiaComMarkup = valorTotalEstadiaComMarkup;
    }

    public Double getValorTotalEstadiaComMarkupBrl() {
        return valorTotalEstadiaComMarkupBrl;
    }

    public void setValorTotalEstadiaComMarkupBrl(Double valorTotalEstadiaComMarkupBrl) {
        this.valorTotalEstadiaComMarkupBrl = valorTotalEstadiaComMarkupBrl;
    }

    public Double getValorTotalEstadiaNet() {
        return valorTotalEstadiaNet;
    }

    public void setValorTotalEstadiaNet(Double valorTotalEstadiaNet) {
        this.valorTotalEstadiaNet = valorTotalEstadiaNet;
    }

    public Double getPercentualTaxaIss() {
        return percentualTaxaIss;
    }

    public void setPercentualTaxaIss(Double percentualTaxaIss) {
        this.percentualTaxaIss = percentualTaxaIss;
    }

    public Double getPercentualTaxaServico() {
        return percentualTaxaServico;
    }

    public void setPercentualTaxaServico(Double percentualTaxaServico) {
        this.percentualTaxaServico = percentualTaxaServico;
    }

    public Double getPercentualTaxaExtra() {
        return percentualTaxaExtra;
    }

    public void setPercentualTaxaExtra(Double percentualTaxaExtra) {
        this.percentualTaxaExtra = percentualTaxaExtra;
    }

    public Double getValorTaxaIss() {
        return valorTaxaIss;
    }

    public void setValorTaxaIss(Double valorTaxaIss) {
        this.valorTaxaIss = valorTaxaIss;
    }

    public Double getValorTaxaServico() {
        return valorTaxaServico;
    }

    public void setValorTaxaServico(Double valorTaxaServico) {
        this.valorTaxaServico = valorTaxaServico;
    }

}

