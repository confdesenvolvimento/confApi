package com.confApi.carros.dto;

import lombok.Data;

import java.util.List;

@Data
public class EmitirCarroResponseDTO {
    private String mensagem;
    private String localizador;
    private String urlVoucherPT;
    private String urlVoucherUS;
    private List<PagamentoCarroResponseHub> pagamentos;
    private Boolean pago;
    private List<String> pagamentoInfo;
    private Object informacao;
    private String tokenSession;
    private String tokenSessionExpiresIn;
    private List<ErrorResponse> error;
    private String returnMessage;
    private Boolean success;
}
