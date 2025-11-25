package com.confApi.hub.hotel.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TarifaDiaria implements Serializable {
    private Date dataTarifa;
    private Integer maximoHospedes;
    private Integer minimoHospedes;
    private Integer maximoCriancas;
    private Integer minimoCriancas;
    private Integer numberOfUnits =0;
    private String status;
    private Double total =0.0;

}

