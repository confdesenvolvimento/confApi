package com.confApi.chatconfianca.dto.remarcacao;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class RemarcacaoSimulacaoResponse {
    private Long id;
    private Long conversaId;
    private String status;
    private String localizador;
    private String companhiaIata;
    private String titulo;
    private String mensagem;
    private String motivoBloqueio;
    private LocalDateTime expiraEm;
    private boolean permiteEncaminhar;
    private boolean permiteSelecionarTodos;
    private boolean exigeFormaPagamento;
    private List<Trecho> trechos = new ArrayList<>();
    private List<Passageiro> passageiros = new ArrayList<>();
    private List<FormaPagamento> formasPagamento = new ArrayList<>();
    private FormaPagamento formaPagamentoSelecionada;
    private Criterios criterios;
    private List<OpcaoVoo> opcoes = new ArrayList<>();
    private Previa previa;

    @Data
    public static class Trecho {
        private Integer indice;
        private String origem;
        private String destino;
        private String companhia;
        private String dataPartida;
        private String horaPartida;
        private String dataChegada;
        private String horaChegada;
        private String numeroVoos;
        /** Detalhamento dos voos que compoem o trecho (inclui conexoes). */
        private List<Voo> voos = new ArrayList<>();
        private boolean selecionado;
    }

    @Data
    public static class Voo {
        private String companhia;
        private String numero;
        private String origem;
        private String destino;
        private String dataPartida;
        private String horaPartida;
        private String dataChegada;
        private String horaChegada;
        private String duracao;
        private String equipamento;
    }

    @Data
    public static class Passageiro {
        private Integer indice;
        private String identificador;
        private String nome;
        private String tipo;
        private boolean selecionado;
        private boolean elegivel;
        private String motivoInelegibilidade;
    }

    @Data
    public static class Criterios {
        private String origem;
        private String destino;
        private String companhia;
        private LocalDate dataMinima;
        private LocalDate dataMaxima;
        private LocalDate dataSugerida;
        private String periodo = "QUALQUER";
        private boolean somenteDireto;
        private String observacao;
    }

    @Data
    public static class OpcaoVoo {
        private Integer indice;
        private String sistema;
        private String companhia;
        private String origem;
        private String destino;
        private String dataPartida;
        private String horaPartida;
        private String dataChegada;
        private String horaChegada;
        private String duracao;
        private Integer paradas;
        private String numerosVoos;
        private List<Voo> voos = new ArrayList<>();
        private BigDecimal menorValor;
        private String moeda = "BRL";
        private List<Familia> familias = new ArrayList<>();
    }

    @Data
    public static class Familia {
        private Integer indice;
        private String nome;
        private String cabine;
        private String classe;
        private String bagagem;
        private BigDecimal valor;
        private String moeda = "BRL";
    }

    @Data
    public static class Previa {
        private OpcaoVoo voo;
        private Familia familia;
        private String moeda = "BRL";
        private BigDecimal tarifaOriginal;
        private BigDecimal novaTarifa;
        private BigDecimal multa;
        private BigDecimal diferencaTarifaria;
        private BigDecimal diferencaTaxas;
        private BigDecimal taxaServico;
        private BigDecimal totalEstimado;
        private BigDecimal totalSelecionado;
        private List<PreviaPassageiro> passageiros = new ArrayList<>();
        private boolean calculoCompleto;
        private String regraResumo;
        private String aviso;
        private LocalDateTime validoAte;
    }

    @Data
    public static class PreviaPassageiro {
        private Integer indice;
        private String identificador;
        private String nome;
        private String tipo;
        private String familiaOriginal;
        private BigDecimal tarifaOriginal;
        private BigDecimal novaTarifa;
        private BigDecimal taxaEmbarqueOriginal;
        private BigDecimal novaTaxaEmbarque;
        private BigDecimal taxaServicoOriginal;
        private BigDecimal novaTaxaServico;
        private BigDecimal multa;
        private BigDecimal diferencaTarifaria;
        private BigDecimal diferencaTaxaEmbarque;
        private BigDecimal taxaDu;
        private BigDecimal totalEstimado;
        private boolean calculoCompleto;
    }

    @Data
    public static class FormaPagamento {
        private Integer codigo;
        private String chave;
        private String descricao;
        private boolean disponivel;
        private String status;
        private String mensagem;
        private LocalDateTime selecionadaEm;
    }
}
