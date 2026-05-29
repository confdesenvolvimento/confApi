package com.confApi.aereo.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
public class ConsultarLocalizadorResponse {
    private Object exception;
    private List<Reserva> reservas = new ArrayList<>();
}
