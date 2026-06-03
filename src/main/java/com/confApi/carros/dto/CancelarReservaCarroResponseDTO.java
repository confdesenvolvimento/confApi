package com.confApi.carros.dto;

import lombok.Data;

import java.util.List;

@Data
public class CancelarReservaCarroResponseDTO {
    private String message;
    private String tokenSession;
    private String tokenSessionExpiresIn;
    private List<ErrorResponse> error;
    private Object returnMessage;
    private Boolean success;
}
