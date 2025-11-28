package com.confApi.endPoints.reservaAereo;

import com.confApi.endPoints.agencia.Agencia;
import com.confApi.geradorPdf.aereo.GeradorAereoPDF;
import lombok.Data;

@Data
public class ReservaAereoConsultarLocalizadorRequest {
    private String sistema;
    private Agencia agencia = null;
    private String localizador;

    public ReservaAereoConsultarLocalizadorRequest(GeradorAereoPDF geradorAereoPDF, Agencia agencia) {
        this.sistema = "Wooba";
        this.agencia = agencia;
        this.localizador = geradorAereoPDF.getReservaLocalizaodr();
    }
}
