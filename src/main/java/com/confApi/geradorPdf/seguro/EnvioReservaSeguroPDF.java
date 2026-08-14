package com.confApi.geradorPdf.seguro;

import com.confApi.db.confManager.usuario.dto.UsuarioConfDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnvioReservaSeguroPDF implements Serializable {

    private static final long serialVersionUID = 1L;

    private ReservaSeguroModelPDF reservaSeguroModelPDF;
    private UsuarioConfDto usuarioConfDto;
    private PlanoViagemReservaSeguroPDF planoViagemReservaSeguroPDF;

    public EnvioReservaSeguroPDF(
            GeradorSeguroPDFModel geradorSeguroPDFModel
    ) {
        if (geradorSeguroPDFModel == null) {
            throw new IllegalArgumentException(
                    "GeradorSeguroPDFModel não pode ser nulo."
            );
        }

        this.reservaSeguroModelPDF =
                geradorSeguroPDFModel.getReservaSeguroModelPDF();

        this.usuarioConfDto =
                geradorSeguroPDFModel.getUsuarioConfDto();

        this.planoViagemReservaSeguroPDF =
                geradorSeguroPDFModel
                        .getPlanoViagemReservaSeguroPDF();
    }
}