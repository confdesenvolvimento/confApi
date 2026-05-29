package com.confApi.carros.dto;

import lombok.Data;

import java.util.List;

@Data
public class SelecionarCarroResponseDTO {
    private String mensagem;
    private Carros carro;
    private List<DocumentosMandatorios> documentosMandatorios = new java.util.ArrayList<>();
    private String codigoCarrinho;
    private String tokenSession;
    private String tokenSessionExpiresIn;
    private List<ErrorResponse> error = new java.util.ArrayList<>();
    private Object returnMessage;
    private Boolean success;
}
