package com.confApi.geradorPdf;

import com.confApi.db.confManager.geradorPdf.ReservaAereoModelPDF;
import com.confApi.geradorPdf.EnvioReservaAereoPDF.EnvioPlanoViagemReservaAereoPDF;
import com.confApi.geradorPdf.EnvioReservaAereoPDF.EnvioReservaAereoPDF;
import com.confApi.geradorPdf.geradorAereoPDF.EnvioPdfService;
import com.confApi.geradorPdf.geradorAereoPDF.GeradorAereoPDFModel;
import com.confApi.geradorPdf.geradorAereoPDF.GeradorReservaAereoPDFService;
import com.confApi.hub.aereo.ReservaAereoModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/geradorPDF")
public class GeradorPDFController {

    @Autowired
    private GeradorReservaAereoPDFService aereoPDFService;

    @PostMapping("/reservaAereo")
    public void geradorPdf(@RequestBody GeradorAereoPDFModel geradorAereoPDFModel) throws IOException {
        System.out.println("getUsuarioConfDto :  "+geradorAereoPDFModel.getUsuarioConfDto());
        System.out.println("plano viagem :  "+geradorAereoPDFModel.getPlanoViagemReservaAereoPDF());

       byte[] pdfBytes = aereoPDFService.gerarPdfReserva(new ReservaAereoModelPDF(geradorAereoPDFModel.getReservaAereoModel()));

        new EnvioPdfService().envioPDF(new EnvioPlanoViagemReservaAereoPDF(geradorAereoPDFModel,pdfBytes));
    }
}
