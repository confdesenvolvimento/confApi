package com.confApi.geradorPdf;

import com.confApi.db.confManager.geradorPdf.ReservaAereoModelPDF;
import com.confApi.geradorPdf.EnvioReservaAereoPDF.EnvioReservaAereoPDF;
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
    public void geradorPdf(@RequestBody ReservaAereoModel reservaAereoModel) throws IOException {
        System.out.println("reservaAereoModel :  "+reservaAereoModel);

        byte[] pdfBytes = aereoPDFService.gerarPdfReserva(new ReservaAereoModelPDF(reservaAereoModel));

        new EnvioPdfService().envioPDF(new EnvioReservaAereoPDF(reservaAereoModel,pdfBytes));
    }
}
