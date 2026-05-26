package com.confApi.carros.dto;

import lombok.Data;

import java.util.List;

@Data
public class Carros {
    private String id;
    private Boolean carroDisponivel;
    private String fornecedor;
    private String companhiaCarroIATA;
    private String companhiaCarroName;
    private String companhiaCarroLogo;
    private String modelo;
    private String imagem;
    private Integer numeroPassageiros;
    private Integer numeroPortas;
    private Integer tamanhoCarro;
    private Integer quantidadeBagagens;
    private Boolean arCondicionado;
    private String transmissao;
    private String streaming;
    private String combustivel;
    private String tipoGrupo;
    private Boolean carroComFranquia;
    private String codigoRate;
    private String classificacaoCarroCode;
    private Integer codigoTarifa;
    private String codigoPromocao;
    private String codigoDesconto;
    private String obsCodigoDesconto;
    private String tipoTarifa;
    private Boolean maisVendido;
    private Boolean carrosPreferidos;
    private List<TaxasItens> taxation  = new java.util.ArrayList<>();
    private String formaPagamento;
    private Object formsOfReceipts;
    private ValoresCarro valoresCarro;
    private ValoresModais valoresModais;
    private RegrasCancelamento regrasCancelamento;
    private Object tipoCarroCodigo;
    private Boolean adicionadoValorTaxaCalculado;
    private Boolean recomendado;
    private String termosCondicoes;
    private Object informacoes;
    private Boolean informacoesCarro;
    private Boolean permiteCartaoFidelidadeCarro;
    private Boolean permiteCartaoFidelidadeAereo;
    private Object informacoesCompanhiaCarro;
    private Boolean tipoCarroSemProtecao;
    private Object mensagemAdicionalFranquia;
    private Object informacoesCartao;
    private Object alertaPagamentoLocal;
    private Boolean proibidoPagamentoDepois;
    private Double valorDescontoAgente;
    private Boolean numeroVooObrigatorio;
    private Boolean numeroTelefoneObrigatorio;
    private Boolean taxaLiquidacao;
    private Boolean isCarroComTaxaAmdinistrativa;
    private List<LojasDisponiveis> lojasRetirada = new java.util.ArrayList<>();
    private List<LojasDisponiveis> lojasDevolucao = new java.util.ArrayList<>();
    private List<CarroItem> carroItems = new java.util.ArrayList<>();
}
