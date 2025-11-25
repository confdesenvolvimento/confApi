package com.confApi.geradorPdf;

import com.confApi.geradorPdf.aereo.EnvioReservaAereoPDF;
import com.confApi.geradorPdf.aereo.GeradorAereoPDFModel;
import com.confApi.geradorPdf.hotel.EnvioReservaHotelPDF;
import com.confApi.geradorPdf.hotel.GeradorHotelPDFModel;

public class GeradorPDFService {

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
}
