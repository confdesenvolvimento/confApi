package com.confApi.cacheHotel;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class MelhoresTarifasAereasIdaVoltaResponse {
    private String status;
    private String motivoSemDados;
    private String moeda;
    private Rota rota;
    private Periodos periodos;
    private Regras regras;
    private PassageirosBase passageirosBase;
    private CombinacaoTarifaAereaIdaVoltaDTO melhorGeral;
    private CombinacaoTarifaAereaIdaVoltaDTO melhorMesmaCompanhia;
    private CombinacaoTarifaAereaIdaVoltaDTO melhorCompanhiasDiferentes;
    private List<CombinacaoTarifaAereaIdaVoltaDTO> alternativasMesmaCompanhia =
            new ArrayList<>();
    private List<CombinacaoTarifaAereaIdaVoltaDTO> alternativasCompanhiasDiferentes =
            new ArrayList<>();

    @Data
    public static class Rota {
        private String origem;
        private String destino;
    }

    @Data
    public static class Periodos {
        private Periodo ida;
        private Periodo volta;
    }

    @Data
    public static class Periodo {
        private LocalDate inicio;
        private LocalDate fim;
    }

    @Data
    public static class Regras {
        private String cabine;
        private Integer duracaoMinimaDias;
        private Integer duracaoMaximaDias;
        private Integer limiteAlternativas;
        private boolean mesmaCabineObrigatoria;
        private String classificacaoCompanhia;
    }

    @Data
    public static class PassageirosBase {
        private Integer adultos;
        private Integer criancas;
        private Integer bebes;
    }
}
