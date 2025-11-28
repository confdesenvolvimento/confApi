package com.confApi.endPoints.regraTarifariaResponse;

import com.confApi.endPoints.aeroporto.AeroportoResponse;
import com.confApi.endPoints.baseTarifaria.BaseTarifariaResponse;
import lombok.Data;

@Data
public class RegraTarifariaResponse {
    private Integer id;
    private BaseTarifariaResponse baseTarifariaWooba;
    private String dataChegada;
    private String dataSaida;
    private AeroportoResponse destinoWooba;
    private String horaChegada;
    private String horaSaida;
    private String numero;
    private AeroportoResponse origemWooba;
    private String regra;
    private Object regraEstruturada;
}
