package com.confApi.aereo.dto;

import lombok.Data;

import java.util.Date;
@Data
public class ConsultarEticketResponse {
    private Date data;
    private String dataVersao;
    private Boolean sessaoExpirada;
    private Bilhete bilhete;
    private ExceptionDetail exception;
    private Reserva reserva;
    private String status;
    // Confirmações por bilhete preservadas mesmo quando a sincronização local falha.
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
    private java.util.List<String> bilhetesCanceladosFornecedor;
}
