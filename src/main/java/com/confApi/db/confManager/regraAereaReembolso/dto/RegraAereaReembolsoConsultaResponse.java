package com.confApi.db.confManager.regraAereaReembolso.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegraAereaReembolsoConsultaResponse {
    private String status;
    private String mensagem;
    private RegraAereaReembolsoRegraResponse regra;
    private RegraAereaReembolsoCalculoResponse calculo;
    private Map<String, Object> match = new HashMap<>();
}
