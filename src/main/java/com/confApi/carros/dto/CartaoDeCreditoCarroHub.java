package com.confApi.carros.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true)
public class CartaoDeCreditoCarroHub {
    private String nomeProprietario;
    @ToString.Include
    private String bandeira;
    private String numeroCartao;
    private Integer validadeMes;
    private Integer validadeAno;
    private String cvv;
    @ToString.Include
    private Integer numeroParcelas;
    private InfoPagadorHub infoPagador;
}
