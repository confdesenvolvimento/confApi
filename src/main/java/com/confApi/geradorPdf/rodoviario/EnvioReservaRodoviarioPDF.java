package com.confApi.geradorPdf.rodoviario;

import com.confApi.db.confManager.usuario.dto.UsuarioConfDto;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EnvioReservaRodoviarioPDF {

    private ReservaRodoviarioModelPDF reservaRodoviarioModelPDF;
    private UsuarioConfDto usuarioConfDto;
    private PlanoViagemReservaRodoviarioPDF planoViagemReservaRodoviarioPDF;

    public EnvioReservaRodoviarioPDF(GeradorRodoviarioPDFModel geradorRodoviarioPDFModel) {
        if (geradorRodoviarioPDFModel == null) {
            return;
        }

        this.reservaRodoviarioModelPDF = geradorRodoviarioPDFModel.getReservaRodoviarioModelPDF();
        this.usuarioConfDto = geradorRodoviarioPDFModel.getUsuarioConfDto();
        this.planoViagemReservaRodoviarioPDF = geradorRodoviarioPDFModel.getPlanoViagemReservaRodoviarioPDF();
    }
}
