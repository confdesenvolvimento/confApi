package com.confApi.cacheHotel;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MelhorTarifaAereaDTO {
    private LocalDate data;
    private String mes;
    private BigDecimal total;
    private String moeda;
    private String cabine;
    private String nomeCabine;
    private String iataCia;
}
