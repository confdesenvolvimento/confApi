package com.confApi.db.confManager.hotel;

import com.confApi.geradorPdf.geradorAereoPDF.EnvioPdfService;
import com.confApi.geradorPdf.aereo.EnvioReservaAereoPDF;
import com.confApi.geradorPdf.aereo.GeradorAereoPDFModel;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/hotel")
public class HotelController {

    @PostMapping("/reservaAereo")
    public void geradorPdf2(@RequestBody GeradorAereoPDFModel geradorAereoPDFModel) throws IOException {
        System.out.println("getUsuarioConfDto :  "+geradorAereoPDFModel.getUsuarioConfDto());
        System.out.println("plano viagem :  "+geradorAereoPDFModel.getPlanoViagemReservaAereoPDF());
        System.out.println("reserva :  "+geradorAereoPDFModel.getReservaAereoModel().getUsuarioCriacao2().getCodigoWooba());

        // byte[] pdfBytes = aereoPDFService.gerarPdfReserva(new ReservaAereoModelPDF(geradorAereoPDFModel.getReservaAereoModel()));

        // new EnvioPdfService().envioPDF(new EnvioPlanoViagemReservaAereoPDF(geradorAereoPDFModel));
        EnvioReservaAereoPDF envio = new EnvioReservaAereoPDF(geradorAereoPDFModel);
        System.out.println("reserva : "+envio.getReservaAereoModelPDF().getLocalizador());
        System.out.println("reserva : "+envio.getReservaAereoModelPDF().getUsuarioCriacao().getLoginUsuario());
        System.out.println("reserva : "+envio.getReservaAereoModelPDF().getUsuarioCriacao().getNomeCompleto());
        new EnvioPdfService().envioPDF2(new EnvioReservaAereoPDF(geradorAereoPDFModel));
    }
}
