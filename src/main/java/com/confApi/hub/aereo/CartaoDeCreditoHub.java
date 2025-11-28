package com.confApi.hub.aereo;

import lombok.Data;

@Data
public class CartaoDeCreditoHub {
    private Integer id = 0;
    private String autorizacao;
    private BandeiraHub bandeira;
    private String codigoDeSeguranca;
    private Boolean ignorarValidacao = true;
    private String numero;
    private String titularCPF;
    private String titularNome;
    private String validade;
    private Integer parcelas = null;
    private Integer financiamentoPagamento = null;
}
