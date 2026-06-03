package com.confApi.carros.dto;

import lombok.Data;

import java.util.List;

@Data
public class FormasPagamentoCarroResponseDTO {
    private Double valorMinimoParcela;
    private Double valorMaximoParcela;
    private List<FormasPagamentoCarroHub> formasPagamento;
    private String tokenSession;
    private String tokenSessionExpiresIn;
    private List<ErrorResponse> error;
    private Object returnMessage;
    private Boolean success;
}
