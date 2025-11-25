package com.confApi.db.confManager.hotel.model;

import com.confApi.hub.aereo.RecebimentoModel;

import java.io.Serializable;

public class HotelVoucher implements Serializable {

    private String nomeAgencia;
    private String urlLogoAgencia;
    private String contatoAgencia;

    private HotelResponse hotel;
    private TarifaHotel tarifaHotel;
    private RecebimentoModel recebimento;

    private String localizador;
    private String checkIn;
    private String checkOut;
    private Integer noites;
    private Integer quantidadeQuartos;
    private Integer adultos;
    private Integer criancas;
    private double taxas = 0.0;
    private double total = 0.0;
    private String hospedeNome;
    private String tipoQuarto;
    private String checkInHorario;
    private String checkOutHorario;
    private String politicasCancelamento;
    private Boolean isMostrarValores = false;
    private Boolean isExibirVoucher = true;
    private Integer statusReserva;

    public Boolean getIsExibirVoucher() {
        return isExibirVoucher;
    }

    public void setIsExibirVoucher(Boolean isExibirVoucher) {
        this.isExibirVoucher = isExibirVoucher;
    }

    public Integer getStatusReserva() {
        return statusReserva;
    }

    public void setStatusReserva(Integer statusReserva) {
        this.statusReserva = statusReserva;
    }



    public String getNomeAgencia() {
        return nomeAgencia;
    }

    public void setNomeAgencia(String nomeAgencia) {
        this.nomeAgencia = nomeAgencia;
    }

    public String getUrlLogoAgencia() {
        return urlLogoAgencia;
    }

    public void setUrlLogoAgencia(String urlLogoAgencia) {
        this.urlLogoAgencia = urlLogoAgencia;
    }

    public String getContatoAgencia() {
        return contatoAgencia;
    }

    public void setContatoAgencia(String contatoAgencia) {
        this.contatoAgencia = contatoAgencia;
    }

    public HotelResponse getHotel() {
        return hotel;
    }

    public void setHotel(HotelResponse hotel) {
        this.hotel = hotel;
    }

    public TarifaHotel getTarifaHotel() {
        return tarifaHotel;
    }

    public void setTarifaHotel(TarifaHotel tarifaHotel) {
        this.tarifaHotel = tarifaHotel;
    }

    public RecebimentoModel getRecebimento() {
        return recebimento;
    }

    public void setRecebimento(RecebimentoModel recebimento) {
        this.recebimento = recebimento;
    }

    public String getLocalizador() {
        return localizador;
    }

    public void setLocalizador(String localizador) {
        this.localizador = localizador;
    }

    public String getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(String checkIn) {
        this.checkIn = checkIn;
    }

    public String getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(String checkOut) {
        this.checkOut = checkOut;
    }

    public Integer getNoites() {
        return noites;
    }

    public void setNoites(Integer noites) {
        this.noites = noites;
    }

    public Integer getQuantidadeQuartos() {
        return quantidadeQuartos;
    }

    public void setQuantidadeQuartos(Integer quantidadeQuartos) {
        this.quantidadeQuartos = quantidadeQuartos;
    }

    public Integer getAdultos() {
        return adultos;
    }

    public void setAdultos(Integer adultos) {
        this.adultos = adultos;
    }

    public Integer getCriancas() {
        return criancas;
    }

    public void setCriancas(Integer criancas) {
        this.criancas = criancas;
    }

    public double getTaxas() {
        return taxas;
    }

    public void setTaxas(double taxas) {
        this.taxas = taxas;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getHospedeNome() {
        return hospedeNome;
    }

    public void setHospedeNome(String hospedeNome) {
        this.hospedeNome = hospedeNome;
    }

    public String getTipoQuarto() {
        return tipoQuarto;
    }

    public void setTipoQuarto(String tipoQuarto) {
        this.tipoQuarto = tipoQuarto;
    }

    public String getCheckInHorario() {
        return checkInHorario;
    }

    public void setCheckInHorario(String checkInHorario) {
        this.checkInHorario = checkInHorario;
    }

    public String getCheckOutHorario() {
        return checkOutHorario;
    }

    public void setCheckOutHorario(String checkOutHorario) {
        this.checkOutHorario = checkOutHorario;
    }

    public String getPoliticasCancelamento() {
        return politicasCancelamento;
    }

    public void setPoliticasCancelamento(String politicasCancelamento) {
        this.politicasCancelamento = politicasCancelamento;
    }

    public Boolean getIsMostrarValores() {
        return isMostrarValores;
    }

    public void setIsMostrarValores(Boolean isMostrarValores) {
        this.isMostrarValores = isMostrarValores;
    }



}

