package com.confApi.geradorPdf.seguro;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class ReservaSeguroModelPDF implements Serializable {

    private Long codgReservaSeguroDB;
    private String localizador;

    private Integer status;
    private String statusDescricao;

    private Date dataCriacao;
    private Date dataEmissao;
    private Date dataCancelamento;

    private Date dataInicioCobertura;
    private Date dataFinalCobertura;

    private Integer statusPagamentoCliente;
    private String statusPagamentoDescricao;

    private String nomeAgencia;
    private String nomeUsuario;

    private BigDecimal valorTotal;

    private CoberturaSeguroPDF cobertura;

    private List<SeguradoSeguroPDF> segurados = new ArrayList<>();

    private List<RecebimentoSeguroPDF> recebimentos = new ArrayList<>();

    private ContatoEmergenciaSeguroPDF contatoEmergencia;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoberturaSeguroPDF implements Serializable {

        private String fornecedor;
        private String idPlano;
        private String nomePlano;
        private String urlLogo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeguradoSeguroPDF implements Serializable {

        private String nome;
        private String sobrenome;
        private String sexo;
        private String cpf;
        private Date nascimento;
        private String telefone;
        private String email;
        private String numeroApolice;
        private String endpointPdf;
        private String paisDestino;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecebimentoSeguroPDF implements Serializable {


        private Integer codgFormaPagamento;
        private String nomeFormaPagamento;

        private Integer statusRecebimento;
        private String statusDescricao;

        private Date dataRecebimento;

        private BigDecimal valorEntrada;
        private BigDecimal valorPagamento;

        private String link;

        private CartaoSeguroPDF cartaoSelecionado;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartaoSeguroPDF implements Serializable {

        /**
         * Enviar somente o número mascarado.
         * Exemplo: **** **** **** 1234.
         */
        private String numeroCartao;

        private Integer quantidadeParcelas;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContatoEmergenciaSeguroPDF implements Serializable {

        private String nome;
        private String telefone;
        private String email;
    }
}