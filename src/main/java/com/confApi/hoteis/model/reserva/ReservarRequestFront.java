package com.confApi.hoteis.model.reserva;

import com.confApi.hub.hotel.dto.InfoGlobal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservarRequestFront {
    private Date dataCriacao;
    private InfoGlobal infoGlobal;
    private String identificador;
    private String status;
    private Date dtCheckIn;
    private Date dtCheckOut;
    private String codgHotel;
    private String codgCidade;
    private List<HotelAcomodacaoFront> acomodacoes;
    private String searchToken;
}
