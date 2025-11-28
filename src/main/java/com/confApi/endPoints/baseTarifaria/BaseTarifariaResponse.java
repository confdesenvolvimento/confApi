package com.confApi.endPoints.baseTarifaria;

import lombok.Data;

@Data
public class BaseTarifariaResponse {
    private Integer id;
    private String classe;
    private String codigo;
    private String familia;
}
