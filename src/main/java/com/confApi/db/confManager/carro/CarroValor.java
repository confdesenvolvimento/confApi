package com.confApi.db.confManager.carro;

import com.confApi.carros.dto.CarroBookingHub;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CarroValor {

    private Integer codgCarroValor;
    private CarroReserva carroReserva;
    private Double valorTotalReserva_net;
    private Double valorTotalReservaMarkup;
    private String codgMoeda;
    private Double valorCambio;
    private Double valorTotalReservaNetBrl;
    private Double valorTotalReservaMarkupBrl;
    private Double percMarkup;
    private Double valorMarkup;
    private Double valorMarkupBrl;
    private Double valorTaxas;
    private Double valorTaxasBrl;
    private Double valorTaxaExtra;
    private Double valorTaxaExtraBrl;
    private Double valorProtecaoNet;
    private Double valorProtecaoNetBrl;
    private Double valorProtecaoMarkup;
    private Double valorProtecaoMarkupBrl;
    private Double valorTarifaNet;
    private Double valorTarifaNetBrl;
    private Double valorTarifaMarkup;
    private Double valorTarifaMarkupBrl;

    public CarroValor(CarroBookingHub obj, CarroReserva carroReserva) {
        this.carroReserva = carroReserva;

        this.valorTotalReserva_net = obj != null ? obj.getValorTotalPagamento() : null;
        this.valorTotalReservaMarkup = null;

        this.codgMoeda = obj != null ? obj.getMoeda() : null;
        this.valorCambio = obj != null ? obj.getCambio() : null;

        this.valorTotalReservaNetBrl = obj != null ? obj.getValorTotalPagamentoEquivalente() : null;
        this.valorTotalReservaMarkupBrl = null;

        this.percMarkup = null;
        this.valorMarkup = null;
        this.valorMarkupBrl = null;

        this.valorTaxas = obj != null ? obj.getTaxa() : null;
        this.valorTaxasBrl = obj != null ? obj.getTaxaEquivalente() : null;

        this.valorTaxaExtra = null;
        this.valorTaxaExtraBrl = null;

        this.valorProtecaoNet = null;
        this.valorProtecaoNetBrl = null;
        this.valorProtecaoMarkup = null;
        this.valorProtecaoMarkupBrl = null;

        this.valorTarifaNet = obj != null ? obj.getValorTarifa() : null;
        this.valorTarifaNetBrl = obj != null ? obj.getValorTarifaEquivalente() : null;

        this.valorTarifaMarkup = null;
        this.valorTarifaMarkupBrl = null;
    }
}
