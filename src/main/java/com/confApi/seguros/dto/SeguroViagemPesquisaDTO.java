package com.confApi.seguros.dto;

import com.confApi.model.IdentificacaoAgenciaModel;
import com.confApi.seguros.model.DestinoSeguro;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SeguroViagemPesquisaDTO implements Serializable {

    private DestinoSeguro destino;
    private IdentificacaoAgenciaModel identificacaoAgenciaModel;

    private String dataInicio;
    private String dataFim;

    private List<PassageiroSeguro> passageiroSeguro;

    private int qtd0a64;
    private int qtd65a75;
    private int qtd76a85;
    private int qtd86a99;
    // Comissão extra
    // P = Percentual, V = Valor
    private String tipoComissaoExtra = "P";
    private double valorComissaoExtra = 0.0;
    // Filtros
    private List<String> fornecedores = new ArrayList<>();

    public DestinoSeguro getDestino() {
        return destino;
    }

    public void setDestino(DestinoSeguro destino) {
        this.destino = destino;
    }

    public IdentificacaoAgenciaModel getIdentificacaoAgenciaModel() {
        return identificacaoAgenciaModel;
    }

    public void setIdentificacaoAgenciaModel(IdentificacaoAgenciaModel identificacaoAgenciaModel) {
        this.identificacaoAgenciaModel = identificacaoAgenciaModel;
    }

    public String getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(String dataInicio) {
        this.dataInicio = dataInicio;
    }

    public String getDataFim() {
        return dataFim;
    }

    public void setDataFim(String dataFim) {
        this.dataFim = dataFim;
    }

    public int getQtd0a64() {
        return qtd0a64;
    }

    public void setQtd0a64(int qtd0a64) {
        this.qtd0a64 = qtd0a64;
    }

    public int getQtd65a75() {
        return qtd65a75;
    }

    public void setQtd65a75(int qtd65a75) {
        this.qtd65a75 = qtd65a75;
    }

    public int getQtd76a85() {
        return qtd76a85;
    }

    public void setQtd76a85(int qtd76a85) {
        this.qtd76a85 = qtd76a85;
    }

    public int getQtd86a99() {
        return qtd86a99;
    }

    public void setQtd86a99(int qtd86a99) {
        this.qtd86a99 = qtd86a99;
    }

    public String getTipoComissaoExtra() {
        return tipoComissaoExtra;
    }

    public void setTipoComissaoExtra(String tipoComissaoExtra) {
        this.tipoComissaoExtra = tipoComissaoExtra;
    }

    public double getValorComissaoExtra() {
        return valorComissaoExtra;
    }

    public void setValorComissaoExtra(double valorComissaoExtra) {
        this.valorComissaoExtra = valorComissaoExtra;
    }

    public List<String> getFornecedores() {
        return fornecedores;
    }

    public void setFornecedores(List<String> fornecedores) {
        this.fornecedores = fornecedores;
    }

    public List<PassageiroSeguro> getPassageiroSeguro() {
        return passageiroSeguro;
    }

    public void setPassageiroSeguro(List<PassageiroSeguro> passageiroSeguro) {
        this.passageiroSeguro = passageiroSeguro;
    }
}
