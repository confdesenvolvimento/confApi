package com.confApi.endPoints.trechoReserva;

import com.confApi.endPoints.aeroporto.AeroportoResponse;
import com.confApi.endPoints.companhiaAerea.CompanhiaResponse;
import com.confApi.endPoints.obterRegraTarifaria.ObterRegraTarifaResponse;
import com.confApi.endPoints.voo.VooResponse;
import com.confApi.hub.aereo.TrechoReservaHub;
import com.confApi.hub.aereo.VooHub;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TrechoReservaResponse {
    private String sistema;
    private CompanhiaResponse companhia;
    private AeroportoResponse destino;
    private Integer duracao;
    private Integer numeroParadas;
    private AeroportoResponse origem;
    private List<VooResponse> voos;
    private String tempoDeDuracao;
    private String identificacaoDaViagem;
    private ObterRegraTarifaResponse obterRegraTarifaResponse = null;

    public TrechoReservaResponse(TrechoReservaHub trechoReservaHub) {
        this.sistema = trechoReservaHub.getSistema();
        this.companhia = new CompanhiaResponse(trechoReservaHub.getCompanhia());
        this.destino = new AeroportoResponse(trechoReservaHub.getDestino());
        this.duracao = trechoReservaHub.getDuracao();
        this.numeroParadas = trechoReservaHub.getNumeroParadas();
        this.origem = new AeroportoResponse(trechoReservaHub.getOrigem());
        this.voos = new ArrayList<>();
        for(VooHub vooHub : trechoReservaHub.getVoos()){
            VooResponse vooResponse = new VooResponse(vooHub);
            voos.add(vooResponse);
        }
        this.tempoDeDuracao = trechoReservaHub.getTempoDeDuracao();
        this.identificacaoDaViagem = trechoReservaHub.getIdentificacaoDaViagem();
        this.obterRegraTarifaResponse = trechoReservaHub.getObterRegraTarifaResponse();
    }
}
