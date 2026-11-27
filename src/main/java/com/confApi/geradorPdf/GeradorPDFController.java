package com.confApi.geradorPdf;

import com.confApi.geradorPdf.aereo.GeradorAereoPDF;
import com.confApi.geradorPdf.aereo.GeradorAereoPDFModel;
import com.confApi.geradorPdf.carro.GeradorCarroPDFModel;
import com.confApi.geradorPdf.hotel.GeradorHotelPDFModel;
import com.confApi.geradorPdf.rodoviario.GeradorRodoviarioPDFModel;
import com.confApi.geradorPdf.seguro.GeradorSeguroPDFModel;
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
       // System.out.println("geradorAereoPdf");
      //  System.out.println("getUsuarioConfDto :  " + geradorAereoPDFModel.getUsuarioConfDto());
       // System.out.println("plano viagem :  " + geradorAereoPDFModel.getPlanoViagemReservaAereoPDF());
       // System.out.println("reserva :  " + geradorAereoPDFModel.getReservaAereoModel().getUsuarioCriacao2().getCodigoWooba());

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
      //  System.out.println("getUsuarioConfDto :  "+geradorAereoPDFModel.getUsuarioConfDto());
      //  System.out.println("plano viagem :  "+geradorAereoPDFModel.getPlanoViagemReservaAereoPDF());
      //  System.out.println("reserva :  "+geradorAereoPDFModel.getReservaAereoModel().getUsuarioCriacao2().getCodigoWooba());
        geradorPDFService.popularAereoPDF(geradorAereoPDFModel);
    }

    @PostMapping("/reservaHotel")
    public void geradorHotelPdf(@RequestBody GeradorHotelPDFModel geradorHotelPDFModel)  {
       // System.out.println("getUsuarioConfDto :  "+geradorHotelPDFModel.getUsuarioConfDto());
      //  System.out.println("getUsuarioConfDto :  "+geradorHotelPDFModel.getReservaHotelModel().getCodgAgencia());
        geradorPDFService.popularHotelPDF(geradorHotelPDFModel);
    }

    @PostMapping("/reservaCarro")
    public void geradorCarroPdf(@RequestBody GeradorCarroPDFModel geradorCarroPDFModel)  {
        geradorPDFService.popularCarroPDF(geradorCarroPDFModel);
    }

    @PostMapping({"/reservaRodoviario", "/reservaRodoviario/"})
    public void geradorRodoviarioPdf(@RequestBody GeradorRodoviarioPDFModel modelo) {
        geradorPDFService.popularRodoviarioPDF(modelo);
    }

    @PostMapping({"/gerarRodoviarioPDF", "/gerarRodoviarioPDF/"})
    public ResponseEntity<byte[]> gerarRodoviarioPDF(@RequestBody GeradorRodoviarioPDFModel modelo) {
        byte[] pdfBytes = geradorPDFService.gerarPopularRodoviarioPDF(modelo);
        String localizador = modelo.getReservaRodoviarioModelPDF().getLocalizador().trim()
                .replaceAll("[^A-Za-z0-9_-]", "_");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(pdfBytes.length);
        headers.setContentDisposition(ContentDisposition.inline()
                .filename("reserva-rodoviaria-" + localizador + ".pdf")
                .build());
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/reservaSeguro")
    public void geradorSeguroPdf(@RequestBody GeradorSeguroPDFModel geradorSeguroPDFModel)  {
        System.out.println("chegou no geradorSeguroPDFModel:: " + geradorSeguroPDFModel);
        geradorPDFService.popularSeguroPDF(geradorSeguroPDFModel);
    }

    @PostMapping("/reservaAereoApp")
    public void geradorPdfApp(@RequestBody GeradorAereoPDF geradorAereoPDF ) throws IOException {
        geradorPDFService.popularAereoPDFApp(geradorAereoPDF);
    }
}
