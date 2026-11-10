package com.confApi.db.confManager.hotel.model;

import com.confApi.db.confManager.usuario.dto.UsuarioDto;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class HotelPreReserva implements Serializable {

    private HotelResponse hotel;
    private List<HotelAcomodacao> acomodacao;
    private Double totalGeral = 0.0;
    private Boolean isPossuiTarifaNaoReembolsavel = false;
    private Boolean isPossuiTarifaPrePagamento = false;
    private Boolean isCientePoliticaNaoReembolsavel = false;
    private Integer codgPacote = null;
    private String searchToken = null;
    private UsuarioDto usuario;

    public Boolean getIsCientePoliticaNaoReembolsavel() {
        return isCientePoliticaNaoReembolsavel;
    }

    public void setIsCientePoliticaNaoReembolsavel(Boolean isCientePoliticaNaoReembolsavel) {
        this.isCientePoliticaNaoReembolsavel = isCientePoliticaNaoReembolsavel;
    }

    public Boolean getIsPossuiTarifaNaoReembolsavel() {
        return isPossuiTarifaNaoReembolsavel;
    }

    public Boolean getIsPossuiTarifaPrePagamento() {
        return isPossuiTarifaPrePagamento;
    }

    public void setIsPossuiTarifaPrePagamento(Boolean isPossuiTarifaPrePagamento) {
        this.isPossuiTarifaPrePagamento = isPossuiTarifaPrePagamento;
    }

    public void setIsPossuiTarifaNaoReembolsavel(Boolean isPossuiTarifaNaoReembolsavel) {
        this.isPossuiTarifaNaoReembolsavel = isPossuiTarifaNaoReembolsavel;
    }

    public Double getTotalGeral() {
        return totalGeral;
    }

    public void setTotalGeral(Double totalGeral) {
        this.totalGeral = totalGeral;
    }

    public HotelResponse getHotel() {
        return hotel;
    }

    public void setHotel(HotelResponse hotel) {
        this.hotel = hotel;
    }

    public List<HotelAcomodacao> getAcomodacao() {
        return acomodacao;
    }

    public void setAcomodacao(List<HotelAcomodacao> acomodacao) {
        this.acomodacao = acomodacao;
    }

    public Integer getCodgPacote() {
        return codgPacote;
    }

    public void setCodgPacote(Integer codgPacote) {
        this.codgPacote = codgPacote;
    }

    public Boolean getPossuiTarifaNaoReembolsavel() {
        return isPossuiTarifaNaoReembolsavel;
    }

    public void setPossuiTarifaNaoReembolsavel(Boolean possuiTarifaNaoReembolsavel) {
        isPossuiTarifaNaoReembolsavel = possuiTarifaNaoReembolsavel;
    }

    public Boolean getPossuiTarifaPrePagamento() {
        return isPossuiTarifaPrePagamento;
    }

    public void setPossuiTarifaPrePagamento(Boolean possuiTarifaPrePagamento) {
        isPossuiTarifaPrePagamento = possuiTarifaPrePagamento;
    }

    public Boolean getCientePoliticaNaoReembolsavel() {
        return isCientePoliticaNaoReembolsavel;
    }

    public void setCientePoliticaNaoReembolsavel(Boolean cientePoliticaNaoReembolsavel) {
        isCientePoliticaNaoReembolsavel = cientePoliticaNaoReembolsavel;
    }

    public String getSearchToken() {
        return searchToken;
    }

    public void setSearchToken(String searchToken) {
        this.searchToken = searchToken;
    }

    public UsuarioDto getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioDto usuario) {
        this.usuario = usuario;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.hotel);
        hash = 19 * hash + Objects.hashCode(this.acomodacao);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HotelPreReserva other = (HotelPreReserva) obj;
        if (!Objects.equals(this.hotel, other.hotel)) {
            return false;
        }
        return Objects.equals(this.acomodacao, other.acomodacao);
    }

    @Override
    public String toString() {
        return "HotelPreReserva{" + "hotel=" + hotel + ", acomodacao=" + acomodacao + ", totalGeral=" + totalGeral + ", isPossuiTarifaNaoReembolsavel=" + isPossuiTarifaNaoReembolsavel + ", isPossuiTarifaPrePagamento=" + isPossuiTarifaPrePagamento + ", isCientePoliticaNaoReembolsavel=" + isCientePoliticaNaoReembolsavel + ", codgPacote=" + codgPacote + '}';
    }


}

