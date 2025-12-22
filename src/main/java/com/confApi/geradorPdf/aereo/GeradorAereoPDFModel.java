package com.confApi.geradorPdf.aereo;

import com.confApi.db.confManager.usuario.dto.UsuarioConfDto;
import com.confApi.hub.aereo.ReservaAereoModel;
import lombok.Data;

@Data
public class GeradorAereoPDFModel {
    private ReservaAereoModel reservaAereoModel;
    private UsuarioConfDto usuarioConfDto;
    private PlanoViagemReservaAereoPDF planoViagemReservaAereoPDF;

    public GeradorAereoPDFModel(ReservaAereoModel reservaAereoModel, UsuarioConfDto usuarioConfDto, PlanoViagemReservaAereoPDF planoViagemReservaAereoPDF) {
        this.reservaAereoModel = reservaAereoModel;
        this.usuarioConfDto = usuarioConfDto;
        this.planoViagemReservaAereoPDF = planoViagemReservaAereoPDF;
    }
}
