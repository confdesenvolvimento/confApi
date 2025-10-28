package com.confApi.geradorPdf.EnvioReservaAereoPDF;

import com.confApi.hub.aereo.ReservaAereoModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnvioReservaAereoPDF {

    private String subject;
    private String from;
    private String titulo;
    private String localizador;
    private String usuarioSolicitante;
    private String email;
    private String descricaoSolicitante;
    private byte[] pdfBytes;

    public EnvioReservaAereoPDF(ReservaAereoModel reservaAereoModel, byte[] pdfBytes) {
        this.subject = "Reserva " + reservaAereoModel.getLocalizador();
        this.from = "Reserva da Intranet";
        this.titulo = "Envio da Reserva em PDF";
        this.localizador = reservaAereoModel.getLocalizador();
        this.usuarioSolicitante = "usuarioSolicitante";
        this.email = "helder@confiancaturismo.com.br";
        this.descricaoSolicitante = "Segue a reserva gerado em pdf";
        this.pdfBytes = pdfBytes;
    }

}
