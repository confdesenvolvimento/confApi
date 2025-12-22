package com.confApi.db.confManager.aeroporto.DTO;

import lombok.Data;

@Data
public class AeroportoParamRq {

    private String iataAeroporto;
    private String nomeAeroporto;
    private String iataPais;
}
