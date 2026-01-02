package com.confApi.hoteis.model.pesquisa;

import com.confApi.db.confManager.sistema.model.SistemaModel;
import com.confApi.model.IdentificacaoAgenciaModel;

import java.util.ArrayList;
import java.util.List;

public class HotelPesquisaModelFront {
    private IdentificacaoAgenciaModel identificacaoAgenciaModel;
    private HotelDestinoFront destinoHotel = new HotelDestinoFront();
    private String dataEntrada;
    private String dataSaida;
    private Integer quantidadeNoites = 1;
    private Integer quantidadeQuartos = 1;
    private Integer codgCidadeDb;
    private String codgCidade;
    private String nomeCidade;
    private String codgEstado;
    private String nomeEstado;
    private String codgPais;
    private String nomePais;
    private List<HotelPesquisaQuartoFront> quartoPesquisa = new ArrayList<HotelPesquisaQuartoFront>();
    private List<SistemaModel> listSistemas = new ArrayList<>();
    private Double valorComissao=0.0;
    private Integer tipoComissao=1;
    private Double percentualComissao=0.0;

    public HotelPesquisaModelFront() {
    }

    public HotelPesquisaModelFront(HotelDestinoFront destinoHotel, String dataEntrada, String dataSaida, Integer quantidadeNoites) {
        this.destinoHotel = destinoHotel;
        this.dataEntrada = dataEntrada;
        this.dataSaida = dataSaida;
        this.quantidadeNoites = quantidadeNoites;
    }

    public HotelPesquisaModelFront(HotelDestinoFront destinoHotel, String dataEntrada, String dataSaida, Integer quantidadeNoites,
                                   String codgCidade, String nomeCidade, String codgEstado, String nomeEstado, String codgPais, String nomePais) {
        this.destinoHotel = destinoHotel;
        this.dataEntrada = dataEntrada;
        this.dataSaida = dataSaida;
        this.quantidadeNoites = quantidadeNoites;
        this.codgCidade = codgCidade;
        this.nomeCidade = nomeCidade;
        this.codgEstado = codgEstado;
        this.nomeEstado = nomeEstado;
        this.codgPais = codgPais;
        this.nomePais = nomePais;
    }

    public List<SistemaModel> getListSistemas() {
        return listSistemas;
    }

    public void setListSistemas(List<SistemaModel> listSistemas) {
        this.listSistemas = listSistemas;
    }

    public HotelDestinoFront getDestinoHotel() {
        return destinoHotel;
    }

    public void setDestinoHotel(HotelDestinoFront destinoHotel) {
        this.destinoHotel = destinoHotel;
    }

    public String getDataEntrada() {
        return dataEntrada;
    }

    public void setDataEntrada(String dataEntrada) {
        this.dataEntrada = dataEntrada;
    }

    public String getDataSaida() {
        return dataSaida;
    }

    public void setDataSaida(String dataSaida) {
        this.dataSaida = dataSaida;
    }

    public Integer getQuantidadeNoites() {
        return quantidadeNoites;
    }

    public void setQuantidadeNoites(Integer quantidadeNoites) {
        this.quantidadeNoites = quantidadeNoites;
    }

    public Integer getQuantidadeQuartos() {
        return quantidadeQuartos;
    }

    public void setQuantidadeQuartos(Integer quantidadeQuartos) {
        this.quantidadeQuartos = quantidadeQuartos;
    }

    public List<HotelPesquisaQuartoFront> getQuartoPesquisa() {
        return quartoPesquisa;
    }

    public void setQuartoPesquisa(List<HotelPesquisaQuartoFront> quartoPesquisa) {
        this.quartoPesquisa = quartoPesquisa;
    }

    public String getCodgCidade() {
        return codgCidade;
    }

    public void setCodgCidade(String codgCidade) {
        this.codgCidade = codgCidade;
    }

    public String getCodgEstado() {
        return codgEstado;
    }

    public void setCodgEstado(String codgEstado) {
        this.codgEstado = codgEstado;
    }

    public String getCodgPais() {
        return codgPais;
    }

    public void setCodgPais(String codgPais) {
        this.codgPais = codgPais;
    }

    public String getNomeCidade() {
        return nomeCidade;
    }

    public void setNomeCidade(String nomeCidade) {
        this.nomeCidade = nomeCidade;
    }

    public String getNomeEstado() {
        return nomeEstado;
    }

    public void setNomeEstado(String nomeEstado) {
        this.nomeEstado = nomeEstado;
    }

    public String getNomePais() {
        return nomePais;
    }

    public void setNomePais(String nomePais) {
        this.nomePais = nomePais;
    }

    @Override
    public String toString() {
        return "HotelPesquisaModel{" + "destinoHotel=" + destinoHotel + ", dataEntrada=" + dataEntrada + ", dataSaida=" + dataSaida + ", quantidadeNoites=" + quantidadeNoites + ", quantidadeQuartos=" + quantidadeQuartos + ", codgCidade=" + codgCidade + ", nomeCidade=" + nomeCidade + ", codgEstado=" + codgEstado + ", nomeEstado=" + nomeEstado + ", codgPais=" + codgPais + ", nomePais=" + nomePais + ", quartoPesquisa=" + quartoPesquisa + ", listSistemas=" + listSistemas + '}';
    }

    public Integer getCodgCidadeDb() {
        return codgCidadeDb;
    }

    public void setCodgCidadeDb(Integer codgCidadeDb) {
        this.codgCidadeDb = codgCidadeDb;
    }

    public Double getValorComissao() {
        return valorComissao;
    }

    public void setValorComissao(Double valorComissao) {
        this.valorComissao = valorComissao;
    }

    public Integer getTipoComissao() {
        return tipoComissao;
    }

    public void setTipoComissao(Integer tipoComissao) {
        this.tipoComissao = tipoComissao;
    }

    public Double getPercentualComissao() {
        return percentualComissao;
    }

    public void setPercentualComissao(Double percentualComissao) {
        this.percentualComissao = percentualComissao;
    }

    public IdentificacaoAgenciaModel getIdentificacaoAgenciaModel() {
        return identificacaoAgenciaModel;
    }

    public void setIdentificacaoAgenciaModel(IdentificacaoAgenciaModel identificacaoAgenciaModel) {
        this.identificacaoAgenciaModel = identificacaoAgenciaModel;
    }
}
