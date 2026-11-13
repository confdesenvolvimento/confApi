package com.confApi.hub.hotel.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class TarifaHotel {
    private Double valorMarkupAplicado = 0.0;
    private Double percentualMarkupAplicado = 0.0;
    private Double valorTotalEstadiaComMarkup = 0.0;
    private Double valorTotalEstadiaComMarkupBrl = 0.0;
    private Double valorTotalEstadiaNet = 0.0;
    private Double mediaDiaria = 0.0;
    private Double percentualTaxaIss = 0.0;
    private Double percentualTaxaServico = 0.0;
    private Double percentualTaxaExtra = 0.0;
    private Double valorTaxaIss = 0.0;
    private Double valorTaxaServico = 0.0;
    private String moeda;
    private String disponibilidade;
    private List<TarifaDiaria> tarifasDiaria = new ArrayList<>();

    public TarifaHotel(com.confApi.db.confManager.hotel.model.TarifaHotel tarifaHotel) {
        this.valorMarkupAplicado = tarifaHotel.getValorMarkupAplicado();
        this.percentualMarkupAplicado = tarifaHotel.getPercentualMarkupAplicado();
        this.valorTotalEstadiaComMarkup = tarifaHotel.getValorTotalEstadiaComMarkup();
        this.valorTotalEstadiaComMarkupBrl = tarifaHotel.getValorTotalEstadiaComMarkupBrl();
        this.valorTotalEstadiaNet = tarifaHotel.getValorTotalEstadiaNet();
        this.mediaDiaria = tarifaHotel.getMediaDiaria();
        this.percentualTaxaIss = tarifaHotel.getPercentualTaxaIss();
        this.percentualTaxaServico = tarifaHotel.getPercentualTaxaServico();
        this.percentualTaxaExtra = tarifaHotel.getPercentualTaxaExtra();
        this.valorTaxaIss = tarifaHotel.getValorTaxaIss();
        this.valorTaxaServico = tarifaHotel.getValorTaxaServico();
        this.moeda = tarifaHotel.getMoeda();
        this.disponibilidade = tarifaHotel.getDisponibilidade();
        this.tarifasDiaria = tarifaHotel.getTarifasDiaria();
    }
}
