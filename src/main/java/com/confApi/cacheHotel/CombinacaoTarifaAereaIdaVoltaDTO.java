package com.confApi.cacheHotel;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CombinacaoTarifaAereaIdaVoltaDTO {
    private MelhorTarifaAereaDTO ida;
    private MelhorTarifaAereaDTO volta;
    private Integer duracaoDias;
    private BigDecimal total;
    private boolean mesmaCompanhia;
    private boolean mesmaCabine;
}
