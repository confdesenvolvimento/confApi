package com.confApi.db.confManager.reservaHotel.dto;
import java.io.Serializable;
import java.sql.Timestamp;


public class EmissaoRQ implements Serializable{
    private Integer codgReservaHotel;
    private Timestamp dataEmissao;
    private Integer codgUsuarioEmissao;

    public EmissaoRQ() {
    }

    public EmissaoRQ(Integer codgReservaHotel, Timestamp dataEmissao, Integer codgUsuarioEmissao) {
        this.codgReservaHotel = codgReservaHotel;
        this.dataEmissao = dataEmissao;
        this.codgUsuarioEmissao = codgUsuarioEmissao;
    }



    public Integer getCodgReservaHotel() {
        return codgReservaHotel;
    }

    public void setCodgReservaHotel(Integer codgReservaHotel) {
        this.codgReservaHotel = codgReservaHotel;
    }

    public Timestamp getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(Timestamp dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public Integer getCodgUsuarioEmissao() {
        return codgUsuarioEmissao;
    }

    public void setCodgUsuarioEmissao(Integer codgUsuarioEmissao) {
        this.codgUsuarioEmissao = codgUsuarioEmissao;
    }


}
