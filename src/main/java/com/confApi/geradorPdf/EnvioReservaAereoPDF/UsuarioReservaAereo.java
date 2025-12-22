package com.confApi.geradorPdf.EnvioReservaAereoPDF;

import com.confApi.hub.aereo.ReservaAereoModel;
import lombok.Data;

@Data
public class UsuarioReservaAereo {

    private String localizador;
    private String usuarioReserva;
    private String agenciaReserva;
    private String unidadeReserva;
    private String statusReserva;

    public UsuarioReservaAereo(ReservaAereoModel reservaAereoModel){
        this.localizador = reservaAereoModel.getLocalizador();
        this.usuarioReserva = reservaAereoModel.getUsuarioCriacao();
        this.agenciaReserva = reservaAereoModel.getAgencia().getNomeAgencia();
        this.unidadeReserva = reservaAereoModel.getAgencia().getCodgUnidade().getNomeUnidade();
        this.statusReserva = reservaAereoModel.getStatusReserva();
    }

}
