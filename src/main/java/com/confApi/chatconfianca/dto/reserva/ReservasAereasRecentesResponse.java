package com.confApi.chatconfianca.dto.reserva;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReservasAereasRecentesResponse {
    private String status = "OK";
    private String mensagem;
    private int quantidade;
    private List<ReservaAereaRecenteItem> reservas = new ArrayList<>();
}
