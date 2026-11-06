package com.confApi.aereo.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
public class ConsultarLocalizadorResponse {
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String tipoTarifa;
    private Object exception;
    private List<Reserva> reservas = new ArrayList<>();
    private Boolean valoresAtualizados;
    private Boolean valoresSincronizados;
}
