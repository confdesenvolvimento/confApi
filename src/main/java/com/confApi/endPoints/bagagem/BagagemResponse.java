package com.confApi.endPoints.bagagem;

import com.confApi.hub.aereo.BagagemHub;
import lombok.Data;

@Data
public class BagagemResponse {
    private String id;
    private Integer quantidade;
    private Double valor;
    private Integer peso;
    private String unidadeDeMedida;

    public BagagemResponse(BagagemHub bagagemHub) {
        this.id = bagagemHub.getId();
        this.quantidade = bagagemHub.getQuantidade();
        this.valor = bagagemHub.getValor();
        this.peso = bagagemHub.getPeso();
        this.unidadeDeMedida = bagagemHub.getUnidadeDeMedida();
    }
}
