package com.confApi.geradorPdf.hotel;

import com.confApi.db.confManager.hotel.model.ReservaHotelModel;
import com.confApi.db.confManager.usuario.UsuarioConfDto;
import lombok.Data;

@Data
public class GeradorHotelPDFModel {
    private ReservaHotelModel reservaHotelModel;
    private UsuarioConfDto usuarioConfDto;
    private PlanoViagemReservaHotelPDF planoViagemReservaHotelPDF;

    public GeradorHotelPDFModel(ReservaHotelModel reservaHotel, UsuarioConfDto usuarioConfDto, PlanoViagemReservaHotelPDF planoViagemReservaHotelPDF) {
        this.reservaHotelModel = reservaHotel;
        this.usuarioConfDto = usuarioConfDto;
        this.planoViagemReservaHotelPDF = planoViagemReservaHotelPDF;

    }
}
