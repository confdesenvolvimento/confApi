package com.confApi.db.confManager.seguro.cobertura;

import com.confApi.db.confManager.seguro.reserva.SeguroReserva;
import lombok.Data;

@Data
public class SeguroCobertura {

    private Integer codgSeguroCobertura;
    private SeguroReserva seguroReserva;
    private String idCobertura;
    private String nomeCobertura;
    private String idGrupo;
    private String moeda;
    private Double valorCobertura;
    private Double valorSeguroNet;
    private Double valorSeguroNetBrl;
    private Double valorSeguroMkp;
    private Double valorSeguroMkpBrl;
    private Double percMkp;
    private Double valorMkpAplicado;
    private Double valorMkpAplicadoBrl;
}
