package com.confApi.geradorPdf;

import com.confApi.geradorPdf.aereo.GeradorAereoPDF;
import com.confApi.geradorPdf.aereo.GeradorAereoPDFModel;
import com.confApi.geradorPdf.hotel.GeradorHotelPDFModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/geradorPDF")
public class GeradorPDFController {

    @Autowired
    private GeradorPDFService geradorPDFService;

    @PostMapping("/gerarAereoPDF")
    public ResponseEntity<byte[]> geradorAereoPdf(@RequestBody GeradorAereoPDFModel geradorAereoPDFModel) throws IOException {
        System.out.println("geradorAereoPdf");
        System.out.println("getUsuarioConfDto :  " + geradorAereoPDFModel.getUsuarioConfDto());
        System.out.println("plano viagem :  " + geradorAereoPDFModel.getPlanoViagemReservaAereoPDF());
        System.out.println("reserva :  " + geradorAereoPDFModel.getReservaAereoModel().getUsuarioCriacao2().getCodigoWooba());

        byte[] pdfBytes = geradorPDFService.gerarPopularAereoPDF(geradorAereoPDFModel);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(pdfBytes.length);
        headers.setContentDisposition(
                ContentDisposition.inline()
                        .filename("reserva-" + geradorAereoPDFModel.getReservaAereoModel().getLocalizador() + ".pdf")
                        .build()
        );

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

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
