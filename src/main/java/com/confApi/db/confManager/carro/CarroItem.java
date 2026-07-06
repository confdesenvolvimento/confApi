package com.confApi.db.confManager.carro;

import com.confApi.carros.dto.CarroBookingItemHub;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarroItem {

    private Integer codgCarroItem;
    private CarroReserva carroReserva;
    private String codgItem;
    private String descricao;
    private Double valorNet;
    private Double valorNetBrl;
    private Double valorMarkup;
    private Double valorMarkupBrl;
    private Integer tipoPagamentoItem;
    private String descricaoPagmentoItem;
    private Integer quantidade;

    public CarroItem(CarroBookingItemHub obj) {
        this(obj, null);
    }

    public CarroItem(CarroBookingItemHub obj, CarroReserva carroReserva) {
        this.codgCarroItem = null;
        this.carroReserva = carroReserva;
        this.codgItem = obj != null ? obj.getCode() : null;
        this.descricao = obj != null ? obj.getDescricao() : null;
        this.valorNet = obj != null ? obj.getValor() : null;
        this.valorNetBrl = obj != null ? obj.getValorEquivalente() : null;
        this.valorMarkup = null;
        this.valorMarkupBrl = null;
        this.tipoPagamentoItem = null;
        this.descricaoPagmentoItem = obj != null ? obj.getTipo() : null;
        this.quantidade = obj != null ? obj.getQuantidade() : null;
    }
}
