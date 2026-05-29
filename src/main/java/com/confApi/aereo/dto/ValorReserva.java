package com.confApi.aereo.dto;

import lombok.Data;

@Data
public class ValorReserva {
    private Double saldoDevedor;
    private Double valor;
    private ValorBase valorBase;
}
