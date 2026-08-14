package com.confApi.geradorPdf.seguro;

import com.confApi.db.confManager.usuario.dto.UsuarioConfDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeradorSeguroPDFModel implements Serializable {

    private ReservaSeguroModelPDF reservaSeguroModelPDF;
    private UsuarioConfDto usuarioConfDto;
    private PlanoViagemReservaSeguroPDF planoViagemReservaSeguroPDF;
}
