package com.confApi.cacheHotel;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class PacoteMelhorOfertaRequest {
    private String origem;
    private List<String> destinos = new ArrayList<>();
    private LocalDate dataIdaInicio;
    private LocalDate dataIdaFim;
    private Integer duracaoMinimaNoites;
    private Integer duracaoMaximaNoites;
    private Integer adultos;
    private Integer quartos;
    private Integer limite;
}
