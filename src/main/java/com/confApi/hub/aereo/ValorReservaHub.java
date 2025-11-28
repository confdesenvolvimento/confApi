package com.confApi.hub.aereo;

import lombok.Data;

@Data
public class ValorReservaHub {
    private Double saldoDevedor;
    private Double valor;
    private ValorBaseHub valorBase;
}
