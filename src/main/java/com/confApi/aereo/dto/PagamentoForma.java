package com.confApi.aereo.dto;

import lombok.Data;

@Data
public class PagamentoForma {
    private CartaoDeCredito cartaoDeCredito;
    private Integer formaDePagamento;
}
