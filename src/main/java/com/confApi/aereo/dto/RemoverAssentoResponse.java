package com.confApi.aereo.dto;

import lombok.Data;

@Data
public class RemoverAssentoResponse {
    private String data;
    private String dataVersao;
    private Boolean sessaoExpirada;
    private Object exception;
    private Boolean sucesso;
}
