package com.confApi.geradorPdf;

import com.confApi.geradorPdf.aereo.GeradorAereoPDF;
import com.confApi.geradorPdf.aereo.ReservaAereoModelPDF;
import com.confApi.geradorPdf.EnvioReservaAereoPDF.EnvioPlanoViagemReservaAereoPDF;
import com.confApi.geradorPdf.geradorAereoPDF.EnvioPdfService;
import com.confApi.geradorPdf.aereo.EnvioReservaAereoPDF;
import com.confApi.geradorPdf.aereo.GeradorAereoPDFModel;
import com.confApi.geradorPdf.geradorAereoPDF.GeradorReservaAereoPDFService;
import com.confApi.geradorPdf.hotel.GeradorHotelPDFModel;
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

    @Autowired
    private GeradorPDFService geradorPDFService;
/*
    @PostMapping("/reservaAereo")
    public void geradorPdf(@RequestBody GeradorAereoPDFModel geradorAereoPDFModel) throws IOException {
        System.out.println("getUsuarioConfDto :  "+geradorAereoPDFModel.getUsuarioConfDto());
        System.out.println("plano viagem :  "+geradorAereoPDFModel.getPlanoViagemReservaAereoPDF());

       byte[] pdfBytes = aereoPDFService.gerarPdfReserva(new ReservaAereoModelPDF(geradorAereoPDFModel.getReservaAereoModel()));

        new EnvioPdfService().envioPDF(new EnvioPlanoViagemReservaAereoPDF(geradorAereoPDFModel,pdfBytes));
    }*/

    @PostMapping("/reservaAereo")
    public void geradorPdf2(@RequestBody GeradorAereoPDFModel geradorAereoPDFModel) throws IOException {
        System.out.println("getUsuarioConfDto :  "+geradorAereoPDFModel.getUsuarioConfDto());
        System.out.println("plano viagem :  "+geradorAereoPDFModel.getPlanoViagemReservaAereoPDF());
        System.out.println("reserva :  "+geradorAereoPDFModel.getReservaAereoModel().getUsuarioCriacao2().getCodigoWooba());
        geradorPDFService.popularAereoPDF(geradorAereoPDFModel);
    }

    @PostMapping("/reservaHotel")
    public void geradorHotelPdf(@RequestBody GeradorHotelPDFModel geradorHotelPDFModel)  {
        System.out.println("getUsuarioConfDto :  "+geradorHotelPDFModel.getUsuarioConfDto());
        System.out.println("getUsuarioConfDto :  "+geradorHotelPDFModel.getReservaHotelModel().getCodgAgencia());
        geradorPDFService.popularHotelPDF(geradorHotelPDFModel);
    }

    @PostMapping("/reservaAereoApp")
    public void geradorPdfApp(@RequestBody GeradorAereoPDF geradorAereoPDF ) throws IOException {
        geradorPDFService.popularAereoPDFApp(geradorAereoPDF);
    }
}
