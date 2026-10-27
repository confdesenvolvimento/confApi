package com.confApi.chatconfianca.reservas;

import com.confApi.chatconfianca.dto.reserva.ReservaAereaRecenteItem;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReservaConsultaPagina {
    public static final String SCHEMA = "chat.reservas-consulta.v1";
    private String schema;
    private String status;
    private Integer codgAgencia;
    private ReservaConsultaFiltros filtrosAplicados;
    private List<ReservaAereaRecenteItem> reservas = new ArrayList<>();
    private long totalReservas;
    private long totalLocalizadores;
    private int page;
    private int size;
    private int totalPages;
    private boolean hasNext;
    private String criterioContagem;
}
