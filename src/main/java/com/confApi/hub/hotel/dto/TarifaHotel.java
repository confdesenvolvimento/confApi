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



}
