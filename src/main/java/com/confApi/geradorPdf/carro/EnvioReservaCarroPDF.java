package com.confApi.geradorPdf.carro;

import com.confApi.db.confManager.usuario.dto.UsuarioConfDto;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EnvioReservaCarroPDF {

    private ReservaCarroModelPDF reservaCarroModelPDF;
    private UsuarioConfDto usuarioConfDto;
    private PlanoViagemReservaCarroPDF planoViagemReservaCarroPDF;

    public EnvioReservaCarroPDF(GeradorCarroPDFModel geradorCarroPDFModel) {
        if (geradorCarroPDFModel == null) {
            return;
        }

        this.reservaCarroModelPDF = geradorCarroPDFModel.getReservaCarroModelPDF();
        this.usuarioConfDto = geradorCarroPDFModel.getUsuarioConfDto();
        this.planoViagemReservaCarroPDF = geradorCarroPDFModel.getPlanoViagemReservaCarroPDF();
    }
}