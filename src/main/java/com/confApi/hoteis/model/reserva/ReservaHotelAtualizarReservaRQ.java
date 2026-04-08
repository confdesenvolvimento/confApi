package com.confApi.hoteis.model.reserva;

import lombok.Data;

@Data
public class ReservaHotelAtualizarReservaRQ {
    private Integer codgReservaHotel;
    private Integer reservaStatus;

    public ReservaHotelAtualizarReservaRQ() {
    }

    public ReservaHotelAtualizarReservaRQ(Integer codgReservaHotel, Integer reservaStatus) {
        this.codgReservaHotel = codgReservaHotel;
        this.reservaStatus = reservaStatus;
    }

    public Integer getCodgReservaHotel() {
        return codgReservaHotel;
    }

    public void setCodgReservaHotel(Integer codgReservaHotel) {
        this.codgReservaHotel = codgReservaHotel;
    }

    public Integer getReservaStatus() {
        return reservaStatus;
    }

    public void setReservaStatus(Integer reservaStatus) {
        this.reservaStatus = reservaStatus;
    }
}
