package com.confApi.cacheHotel;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MelhoresTarifasAereasIdaVoltaRequest {
    private String origem;
    private String destino;
    private LocalDate dataIdaInicio;
    private LocalDate dataIdaFim;
    private LocalDate dataVoltaInicio;
    private LocalDate dataVoltaFim;
    private String cabine;
    private Integer duracaoMinimaDias;
    private Integer duracaoMaximaDias;
    private Integer limiteAlternativas;
}
