package com.confApi.geradorPdf;

import com.confApi.db.confManager.usuario.dto.UsuarioConfDto;
import com.confApi.endPoints.agencia.Agencia;
import com.confApi.endPoints.reservaAereo.ReservaAereoConsultarLocalizadorRequest;
import com.confApi.endPoints.reservaAereo.ReservaAereoResponse;
import com.confApi.endPoints.reservaAereo.ReservaAereoService;
import com.confApi.endPoints.usuario.UsuarioResponse;
import com.confApi.endPoints.usuario.UsuarioService;
import com.confApi.geradorPdf.aereo.EnvioReservaAereoPDF;
import com.confApi.geradorPdf.aereo.GeradorAereoPDF;
import com.confApi.geradorPdf.aereo.GeradorAereoPDFModel;
import com.confApi.geradorPdf.aereo.PlanoViagemReservaAereoPDF;
import com.confApi.geradorPdf.hotel.EnvioReservaHotelPDF;
import com.confApi.geradorPdf.hotel.GeradorHotelPDFModel;
import com.confApi.hub.aereo.ReservaAereoModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GeradorPDFService {

    @Autowired
    private ReservaAereoService reservaAereoService;
    @Autowired
    private UsuarioService usuarioService;

    public void popularAereoPDF(GeradorAereoPDFModel geradorAereoPDFModel) {
        EnvioReservaAereoPDF envioReservaAereoPDF = new EnvioReservaAereoPDF(geradorAereoPDFModel);
        new GeradorPDFApi().envioAereoPDF(envioReservaAereoPDF);
    }

    public void popularHotelPDF(GeradorHotelPDFModel geradorHotelPDFModel) {
        System.out.println("getUsuarioConfDto :  "+geradorHotelPDFModel.getUsuarioConfDto());
        EnvioReservaHotelPDF envioReservaHotelPDF = new EnvioReservaHotelPDF(geradorHotelPDFModel);
        System.out.println("reserva : "+envioReservaHotelPDF.getReservaHotelModelPDF().getLocalizador());
        new GeradorPDFApi().envioHotelPDF(envioReservaHotelPDF);
    }

    public void popularAereoPDFApp(GeradorAereoPDF geradorAereoPDF) {

        UsuarioResponse usuarioResponse = usuarioService.consultarUsuario(geradorAereoPDF.getUsuarioLogin());
        usuarioResponse.setAgencia(new Agencia(geradorAereoPDF.getAgenciaCodg()));
        System.out.println("usuarioResponse : "+usuarioResponse);

        ReservaAereoResponse reservaAereoResponse = reservaAereoService.consultarLocalizador(
                new ReservaAereoConsultarLocalizadorRequest(geradorAereoPDF, usuarioResponse.getAgencia())
        );

        GeradorAereoPDFModel geradorAereoPDFModel = new GeradorAereoPDFModel(
                new ReservaAereoModel(reservaAereoResponse),
                new UsuarioConfDto(usuarioResponse),
                new PlanoViagemReservaAereoPDF(geradorAereoPDF)
        );

        EnvioReservaAereoPDF envioReservaAereoPDF = new EnvioReservaAereoPDF(geradorAereoPDFModel);
        new GeradorPDFApi().envioAereoPDF(envioReservaAereoPDF);
    }
}

