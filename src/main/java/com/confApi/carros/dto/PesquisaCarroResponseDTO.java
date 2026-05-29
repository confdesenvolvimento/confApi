package com.confApi.carros.dto;

import lombok.Data;

import java.util.List;

@Data
public class PesquisaCarroResponseDTO {
    private DadosCarroPesquisaDTO dadosPesquisaCarro;
    private List<Resultado> results  = new java.util.ArrayList<>();
    private String tokenSession;
    private String tokenSessionExpiresIn;
    private List<ErrorResponse> error;
    private Object returnMessage;
    private Boolean success;
}
