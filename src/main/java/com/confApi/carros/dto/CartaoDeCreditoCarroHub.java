package com.confApi.carros.dto;

import lombok.Data;

@Data
public class CartaoDeCreditoCarroHub {
    private String nomeProprietario;
    private String bandeira;
    private String numeroCartao;
    private Integer validadeMes;
    private Integer validadeAno;
    private String cvv;
    private Integer numeroParcelas;
    private InfoPagadorHub infoPagador;
}
