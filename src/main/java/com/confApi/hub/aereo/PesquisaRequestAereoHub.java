package com.confApi.hub.aereo;

import com.confApi.endPoints.reservaAereo.ReservaAereoConsultarLocalizadorRequest;
import com.confApi.hub.enumerador.TipoPesquisaHub;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class PesquisaRequestAereoHub {
    private String agencia;
    private String unidade;
    private TipoPesquisaHub tipoPesquisa;
    private TipoPesquisaHub tipoConsulta;
    private AeroportoHub aeroportoOrigem;
    private AeroportoHub aeroportoDestino;
    private Date dataIda;
    private Date dataVolta;
    private List<MultiplostrechoHub> multiplosTrechos = new ArrayList<>();
    private Integer qtdADT = 0;
    private Integer qtdCHD = 0;
    private Integer qtdINF = 0;
    private ParametrosPesquisaHub pesquisa;
    private List<SistemaHub> sistemas;
    private List<CompanhiaHub> companhias;

}
