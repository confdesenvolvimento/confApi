package com.confApi.aereo.dto;

import com.confApi.hub.aereo.CartaoModel;
import com.confApi.hub.aereo.ParcelaCartaoModel;
import com.confApi.hub.aereo.ReservaAereoModel;
import com.confApi.model.IdentificacaoAgenciaModel;
import lombok.Data;

@Data
public class EmitirRequest {
    private IdentificacaoAgenciaModel identificacaoAgenciaModel;
    private String sistema;
    private Agencia agencia;
    private String localizador;
    private String bandeiraCartao;
    private String codigoDeSeguranca;
    private Integer financiamentoId;
    private String numeroCartao;
    private Integer parcelas;
    private String titularNome;
    private String validade;
    private Integer formaDePagamento;
    private Double valor;
    private String cpf;

    public EmitirRequest() {
    }

    public EmitirRequest(IdentificacaoAgenciaModel identificacaoAgenciaModel, String sistema,
                         Agencia agencia, String localizador, String bandeiraCartao,
                         String codigoDeSeguranca, Integer financiamentoId, String numeroCartao,
                         Integer parcelas, String titularNome, String validade, Integer formaDePagamento,
                         Double valor, String cpf) {
        this.identificacaoAgenciaModel = identificacaoAgenciaModel;
        this.sistema = sistema;
        this.agencia = agencia;
        this.localizador = localizador;
        this.bandeiraCartao = bandeiraCartao;
        this.codigoDeSeguranca = codigoDeSeguranca;
        this.financiamentoId = financiamentoId;
        this.numeroCartao = numeroCartao;
        this.parcelas = parcelas;
        this.titularNome = titularNome;
        this.validade = validade;
        this.formaDePagamento = formaDePagamento;
        this.valor = valor;
        this.cpf = cpf;
    }

    public EmitirRequest(ReservaAereoModel reservaAerea) {

        this.sistema = reservaAerea.getSistema();
        this.localizador = reservaAerea.getLocalizador();

        if (reservaAerea.getAgencia() != null) {
            this.agencia = new Agencia(reservaAerea.getAgencia());
        }

        if (reservaAerea.getFormaPagamentoSelecionada() != null) {
            this.formaDePagamento =
                    reservaAerea.getFormaPagamentoSelecionada().getCodgFormaPagto();
        }

        if (reservaAerea.getRecebimento() != null) {
            this.valor = reservaAerea.getRecebimento().getValorPagamento();
        }

        if (Integer.valueOf(2).equals(this.formaDePagamento)
                && reservaAerea.getRecebimento() != null
                && reservaAerea.getRecebimento().getCartaoSelecionado() != null) {

            CartaoModel cartao = reservaAerea.getRecebimento().getCartaoSelecionado();

            this.bandeiraCartao = cartao.getSiglaBandeira();
            this.codigoDeSeguranca = cartao.getCodgSegurancaCartao();
            this.numeroCartao = cartao.getNumeroCartao();
            this.titularNome = cartao.getTitularBandeira();
            this.validade = cartao.getValidadeCartao();
            this.financiamentoId = 0;

            if (cartao.getParcelasCartao() != null
                    && cartao.getQuantidadeParcelas() != null) {

                try {
                    Integer quantidadeParcelas =
                            Integer.valueOf(cartao.getQuantidadeParcelas());

                    for (ParcelaCartaoModel parcela : cartao.getParcelasCartao()) {
                        if (parcela != null
                                && parcela.getNumeroDaParcela() != null
                                && quantidadeParcelas.equals(parcela.getNumeroDaParcela())) {

                            this.parcelas = parcela.getNumeroDaParcela();
                            break;
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }
}
