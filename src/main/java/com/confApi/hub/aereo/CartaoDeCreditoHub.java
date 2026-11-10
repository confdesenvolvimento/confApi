package com.confApi.hub.aereo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true)
public class CartaoDeCreditoHub {
    @ToString.Include
    private Integer id = 0;
    private String autorizacao;
    @ToString.Include
    private BandeiraHub bandeira;
    private String codigoDeSeguranca;
    private Boolean ignorarValidacao = true;
    private String numero;
    private String titularCPF;
    private String titularNome;
    private String validade;
    @ToString.Include
    private Integer parcelas = null;
    private Integer financiamentoPagamento = null;
}
