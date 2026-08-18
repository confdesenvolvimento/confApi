package com.confApi.cacheHotel;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MelhoresTarifasAereasRequest {
    private String origem;
    private String destino;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String cabine;
    private Integer limiteAlternativas;
}
