package com.confApi.endPoints.assento;

import com.confApi.hub.aereo.AssentoHub;
import lombok.Data;

@Data
public class AssentoResponse {
    private String assentoColuna;
    private String assentoLinha;
    private String paxID;
    private String trechoID;
    private String voo;
    private String passageiro;
    private Double valor;
    private String trecho;

    public AssentoResponse(AssentoHub assentoHub) {
        this.assentoColuna = assentoHub.getAssentoColuna();
        this.assentoLinha = assentoHub.getAssentoLinha();
        this.paxID = assentoHub.getPaxID();
        this.trechoID = assentoHub.getTrechoID();
        this.voo = assentoHub.getVoo();
        this.passageiro = assentoHub.getPassageiro();
        this.valor = assentoHub.getValor();
        this.trecho = assentoHub.getTrecho();
    }
}
