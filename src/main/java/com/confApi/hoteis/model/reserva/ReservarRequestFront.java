package com.confApi.hoteis.model.reserva;

import com.confApi.db.confManager.hotel.model.HotelAcomodacao;
import com.confApi.db.confManager.hotel.model.HotelPreReserva;
import com.confApi.hub.hotel.dto.InfoGlobal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
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

    public ReservarRequestFront(HotelPreReserva hotelPreReserva) {
        this.dataCriacao = null;
        this.infoGlobal = null;
        this.identificador = null;
        this.status = null;
        this.dtCheckIn = hotelPreReserva.getHotel().getDataEntrada();
        this.dtCheckOut = hotelPreReserva.getHotel().getDataSaida();
        this.codgHotel = hotelPreReserva.getHotel().getCodigoHotelSistema();
        this.codgCidade = hotelPreReserva.getHotel().getCodigoCidade();
        this.searchToken = hotelPreReserva.getSearchToken();

        this.acomodacoes = new ArrayList<>();
        for(HotelAcomodacao hotelAcomodacao : hotelPreReserva.getAcomodacao()){
            HotelAcomodacaoFront hotelAcomodacaoFront = new HotelAcomodacaoFront(hotelAcomodacao);
            this.acomodacoes.add(hotelAcomodacaoFront);
        }
    }
}
