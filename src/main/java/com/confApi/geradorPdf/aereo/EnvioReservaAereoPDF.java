package com.confApi.geradorPdf.aereo;

import com.confApi.db.confManager.usuario.UsuarioConfDto;
import lombok.Data;

@Data
public class EnvioReservaAereoPDF {
    private ReservaAereoModelPDF reservaAereoModelPDF;
    private UsuarioConfDto usuarioConfDto;
    private PlanoViagemReservaAereoPDF planoViagemReservaAereoPDF;

    public EnvioReservaAereoPDF(GeradorAereoPDFModel geradorAereoPDFModel) {
        this.reservaAereoModelPDF = new ReservaAereoModelPDF(geradorAereoPDFModel.getReservaAereoModel());
        this.usuarioConfDto = geradorAereoPDFModel.getUsuarioConfDto();
        this.planoViagemReservaAereoPDF = geradorAereoPDFModel.getPlanoViagemReservaAereoPDF();

    }

}
