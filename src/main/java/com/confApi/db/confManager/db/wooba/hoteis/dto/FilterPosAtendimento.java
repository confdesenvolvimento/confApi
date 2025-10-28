package com.confApi.db.confManager.db.wooba.hoteis.dto;

import lombok.Data;

import java.io.Serializable;
@Data
public class FilterPosAtendimento  implements Serializable {

    private String dataInicio;
    private String dataFim;
    private String rota;
    private String codgAgencia;
}
