package com.confApi.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
@Data
public class RecebimentoModel implements Serializable {

    private Integer codgFormaPagamento;
    private String nomeFormaPagamento;
    private Double valorEntrada = 0.0;
    private Double valorPagamento = 0.0;
    private FormaPagamentoModel formaDePagamento;
    private CartaoModel cartaoSelecionado;
    private Integer statusRecebimento;
    private Date dataRecebimento;
    private Integer codgCodgRecebimento;
    private String assinatura;
    private String link;
}
