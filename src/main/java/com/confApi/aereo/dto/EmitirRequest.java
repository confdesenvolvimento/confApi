package com.confApi.aereo.dto;

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

}
