package com.confApi.hub.aereo;

import com.confApi.endPoints.obterRegraTarifaria.ObterRegraTarifaResponse;
import lombok.Data;

import java.util.List;

@Data
public class TrechoReservaHub {
    private String sistema;
    private CompanhiaHub companhia;
    private AeroportoHub destino;
    private Integer duracao;
    private Integer numeroParadas;
    private AeroportoHub origem;
    private List<VooHub> voos;
    private String tempoDeDuracao;
    private String identificacaoDaViagem;
    private ObterRegraTarifaResponse obterRegraTarifaResponse = null;

}
