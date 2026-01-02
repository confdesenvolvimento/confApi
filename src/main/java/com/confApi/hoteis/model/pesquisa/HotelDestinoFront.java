package com.confApi.hoteis.model.pesquisa;

import java.io.Serializable;

public class HotelDestinoFront implements Serializable {
    private String nomeHotel;
    private Boolean isHotel = false;
    private String descricao ="";
    private String codgHotel ;

    public String getNomeHotel() {
        return nomeHotel;
    }

    public void setNomeHotel(String nomeHotel) {
        this.nomeHotel = nomeHotel;
    }

    public Boolean getIsHotel() {
        return isHotel;
    }

    public void setIsHotel(Boolean isHotel) {
        this.isHotel = isHotel;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCodgHotel() {
        return codgHotel;
    }

    public void setCodgHotel(String codgHotel) {
        this.codgHotel = codgHotel;
    }

}
