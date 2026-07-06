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
    private String combustivel;
    private String transmissao;
    private Boolean arCondicionado;
    private Integer qtdPassageiros;
    private Integer qtdBagagens;
    private String grupo;
    private String descricaoGrupo;
    private String imagem;
    private String moeda;
    private Double cambio;
    private Double valorTarifa;
    private Double valorTarifaEquivalente;
    private Double valorTotalPagamento;
    private Double valorTotalPagamentoEquivalente;
    private Double taxa;
    private Double taxaEquivalente;
    private String dataRetirada;
    private String dataRetorno;
    private String horaRetirada;
    private String horaRetorno;
    private String veiculoMotorista;
    private String restricao;
    private String restricaoUS;
    private String termosECondicoes;
    private String urlVoucherPT;
    private String urlVoucherUS;
    private String informacoesEmpresaLocadora;
    private List<CarroBookingCondutorHub> condutor = new ArrayList<>();
    private List<CarroBookingItemHub> items = new ArrayList<>();
    private List<CarroBookingLojaHub> lojas = new ArrayList<>();
    private List<CarroBookingCommissaoHub> comissoes = new ArrayList<>();
    private List<CarroBookingAuditHub> info = new ArrayList<>();
}
