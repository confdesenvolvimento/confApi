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
}
