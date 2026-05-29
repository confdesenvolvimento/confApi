package com.confApi.aereo.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;
@Data
public class EmitirResponse {
    private Date data;
    private String dataVersao;
    private Boolean sessaoExpirada;
    private Object cobrancaDeServico;
    private String codigoDeAutorizacao;
    private List<Bilhete> bilhetes;
    private ExceptionDetail exception;
    private Object nivelAnaliseDeRisco;

}
