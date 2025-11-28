package com.confApi.hub.aereo;

import com.confApi.endPoints.reservaAereo.ReservaAereoConsultarLocalizadorRequest;
import lombok.Data;

@Data
public class ConsultarLocalizadorRequestHub {
    private String sistema;
    private AgenciaHub agencia;
    private String localizador;

    public ConsultarLocalizadorRequestHub(ReservaAereoConsultarLocalizadorRequest obj) {
        this.sistema = obj.getSistema();
        this.agencia = obj.getAgencia() != null ? new AgenciaHub(obj.getAgencia()) : null;
        this.localizador = obj.getLocalizador();
    }
}
