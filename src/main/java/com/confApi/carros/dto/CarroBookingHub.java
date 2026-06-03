package com.confApi.carros.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CarroBookingHub {
    private String bookingID;
    private String voucher;
    private String bookingStatus;
    private String pagamentoStatus;
    private String fornecedorNome;
    private String fornecedorIata;
    private String modelo;
    private String imagem;
    private String moeda;
    private Double valorTarifa;
    private Double valorTarifaEquivalente;
    private Double valorTotalPagamento;
    private Double valorTotalPagamentoEquivalente;
    private String dataRetirada;
    private String dataRetorno;
    private String horaRetirada;
    private String horaRetorno;
    private String veiculoMotorista;
    private String restricao;
    private String termosECondicoes;
    private String informacoesEmpresaLocadora;
    private List<CarroBookingCondutorHub> condutor = new ArrayList<>();
    private List<CarroBookingItemHub> items = new ArrayList<>();
    private List<CarroBookingLojaHub> lojas = new ArrayList<>();
    private List<CarroBookingCommissaoHub> comissoes = new ArrayList<>();
    private List<CarroBookingAuditHub> info = new ArrayList<>();
}
