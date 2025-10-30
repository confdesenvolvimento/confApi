package com.confApi.geradorPdf.geradorAereoPDF;

import com.confApi.db.confManager.usuario.UsuarioConfDto;
import com.confApi.hub.aereo.ReservaAereoModel;
import lombok.Data;

@Data
public class GeradorAereoPDFModel {
    private ReservaAereoModel reservaAereoModel;
    private UsuarioConfDto usuarioConfDto;
    private PlanoViagemReservaAereoPDF planoViagemReservaAereoPDF;
}
