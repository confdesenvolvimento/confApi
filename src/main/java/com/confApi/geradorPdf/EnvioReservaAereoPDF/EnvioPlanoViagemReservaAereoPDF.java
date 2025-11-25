package com.confApi.geradorPdf.EnvioReservaAereoPDF;

import com.confApi.db.confManager.usuario.UsuarioConfDto;
import com.confApi.geradorPdf.aereo.GeradorAereoPDFModel;
import com.confApi.geradorPdf.aereo.PlanoViagemReservaAereoPDF;
import lombok.Data;

@Data
public class EnvioPlanoViagemReservaAereoPDF {
    private PlanoViagemReservaAereoPDF planoViagemReservaAereoPDF;
    private UsuarioConfDto usuarioConfDto;
    private UsuarioReservaAereo usuarioReservaAereo;
    private byte[] pdfBytes;


    public EnvioPlanoViagemReservaAereoPDF(GeradorAereoPDFModel geradorAereoPDFModel,  byte[] pdfBytes) {
        this.planoViagemReservaAereoPDF = geradorAereoPDFModel.getPlanoViagemReservaAereoPDF();
        this.usuarioConfDto = geradorAereoPDFModel.getUsuarioConfDto();
        this.usuarioReservaAereo = new UsuarioReservaAereo(geradorAereoPDFModel.getReservaAereoModel());
        this.pdfBytes = pdfBytes;
    }
}
