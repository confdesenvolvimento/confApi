package com.confApi.cacheHotel;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class MelhoresTarifasAereasResponse {
    private String status;
    private String moeda;
    private String origem;
    private String destino;
    private String cabine;
    private LocalDate periodoInicio;
    private LocalDate periodoFim;
    private MelhorTarifaAereaDTO melhorGeral;
    private List<MelhorTarifaAereaDTO> melhoresPorDia = new ArrayList<>();
    private List<MelhorTarifaAereaDTO> melhoresPorCabine = new ArrayList<>();
    private List<MelhorTarifaAereaDTO> melhoresPorMesECabine = new ArrayList<>();
    private List<MelhorTarifaAereaDTO> alternativas = new ArrayList<>();
}
