package com.confApi.geradorPdf.carro;

import com.confApi.db.confManager.usuario.dto.UsuarioConfDto;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GeradorCarroPDFModel {

    private ReservaCarroModelPDF reservaCarroModelPDF;
    private UsuarioConfDto usuarioConfDto;
    private PlanoViagemReservaCarroPDF planoViagemReservaCarroPDF;

    public GeradorCarroPDFModel(ReservaCarroModelPDF reservaCarroModelPDF, UsuarioConfDto usuarioConfDto, PlanoViagemReservaCarroPDF planoViagemReservaCarroPDF) {
        this.reservaCarroModelPDF = reservaCarroModelPDF;
        this.usuarioConfDto = usuarioConfDto;
        this.planoViagemReservaCarroPDF = planoViagemReservaCarroPDF;
    }
}