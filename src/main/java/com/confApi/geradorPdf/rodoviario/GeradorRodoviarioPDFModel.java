package com.confApi.geradorPdf.rodoviario;

import com.confApi.db.confManager.usuario.dto.UsuarioConfDto;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GeradorRodoviarioPDFModel {

    private ReservaRodoviarioModelPDF reservaRodoviarioModelPDF;
    private UsuarioConfDto usuarioConfDto;
    private PlanoViagemReservaRodoviarioPDF planoViagemReservaRodoviarioPDF;

    public GeradorRodoviarioPDFModel(ReservaRodoviarioModelPDF reservaRodoviarioModelPDF, UsuarioConfDto usuarioConfDto, PlanoViagemReservaRodoviarioPDF planoViagemReservaRodoviarioPDF) {
        this.reservaRodoviarioModelPDF = reservaRodoviarioModelPDF;
        this.usuarioConfDto = usuarioConfDto;
        this.planoViagemReservaRodoviarioPDF = planoViagemReservaRodoviarioPDF;
    }
}
