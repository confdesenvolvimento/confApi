package com.confApi.hoteis.dto;

import com.confApi.db.confManager.hotel.model.ReservaHotelModel;
import com.confApi.db.confManager.usuario.Usuario;
import lombok.Data;

import java.util.Date;

@Data
public class ReservaHotelCancelamentoDto {
    private Long codgReservaHotel;
    private String localizador;
    private Date dataCancelamento;
    private String descMotivoCancelamento;
    private Long codgUsuarioCancelamento;

    public ReservaHotelCancelamentoDto(ReservaHotelModel reservaHotelModel, Usuario usuario) {
        this.codgReservaHotel = reservaHotelModel.getCodgReservaHotelDB();
        this.localizador = reservaHotelModel.getLocalizador();
        this.dataCancelamento = reservaHotelModel.getDataCancelamento();
        this.descMotivoCancelamento = reservaHotelModel.getMotivoCancelamento();
        this.codgUsuarioCancelamento = Long.valueOf(usuario.getCodgUsuario());
    }

    public ReservaHotelCancelamentoDto() {
    }

    public ReservaHotelCancelamentoDto(Long codgReservaHotel, String localizador, Date dataCancelamento,
                                       String descMotivoCancelamento, Long codgUsuarioCancelamento) {
        this.codgReservaHotel = codgReservaHotel;
        this.localizador = localizador;
        this.dataCancelamento = dataCancelamento;
        this.descMotivoCancelamento = descMotivoCancelamento;
        this.codgUsuarioCancelamento = codgUsuarioCancelamento;
    }
}
