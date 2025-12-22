package com.confApi.geradorPdf.hotel;

import com.confApi.db.confManager.usuario.dto.UsuarioConfDto;
import lombok.Data;

@Data
public class EnvioReservaHotelPDF {
    private ReservaHotelModelPDF reservaHotelModelPDF;
    private UsuarioConfDto usuarioConfDto;
    private PlanoViagemReservaHotelPDF planoViagemReservaHotelPDF;

    public EnvioReservaHotelPDF(GeradorHotelPDFModel geradorHotelPDFModel) {
        this.reservaHotelModelPDF = new ReservaHotelModelPDF(geradorHotelPDFModel.getReservaHotelModel());
        this.usuarioConfDto = geradorHotelPDFModel.getUsuarioConfDto();
        this.planoViagemReservaHotelPDF = geradorHotelPDFModel.getPlanoViagemReservaHotelPDF();

    }
}
