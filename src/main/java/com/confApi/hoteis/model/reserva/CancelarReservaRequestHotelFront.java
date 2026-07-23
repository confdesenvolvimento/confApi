package com.confApi.hoteis.model.reserva;

import com.confApi.db.confManager.hotel.model.ReservaHotelModel;
import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Data
public class CancelarReservaRequestHotelFront {
    private String sistema;
    private String localizador;
    private String codgHotel;
    private String dataEntrada;
    private String dataSaida;
    private String motivo;

    public CancelarReservaRequestHotelFront() {
    }

    public CancelarReservaRequestHotelFront(String sistema, String localizador, String codgHotel,
                                            String dataEntrada, String dataSaida, String motivo) {
        this.sistema = sistema;
        this.localizador = localizador;
        this.codgHotel = codgHotel;
        this.dataEntrada = dataEntrada;
        this.dataSaida = dataSaida;
        this.motivo = motivo;
    }



    public CancelarReservaRequestHotelFront(ReservaHotelModel reservaHotelModel) {
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd");
        formato.setTimeZone(TimeZone.getTimeZone("UTC"));

        this.sistema = reservaHotelModel.getNomeSistema();
        this.localizador = reservaHotelModel.getLocalizador();
        this.codgHotel = reservaHotelModel.getCodgHotel();

        Date dataEntrada = reservaHotelModel.getHotel().getDataEntrada();
        Date dataSaida = reservaHotelModel.getHotel().getDataSaida();

        this.dataEntrada = dataEntrada != null
                ? formato.format(dataEntrada)
                : null;

        this.dataSaida = dataSaida != null
                ? formato.format(dataSaida)
                : null;

        this.motivo = reservaHotelModel.getMotivoCancelamento();
    }
}
