package com.confApi.aereo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
@Data
public class CancelarReservaResponse {
    private Date data;
    private String dataVersao;
    private Boolean sessaoExpirada;
    private Boolean cancelaReservaEticket;
    private ExceptionDetail exception;
    @JsonProperty("mensagemSucesso")
    private String mensagemSucesso;
    private String status;
}
