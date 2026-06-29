package com.confApi.db.confManager.regraAereaAlteracao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegraAereaAlteracaoConsultaResponse {
    private String status;
    private String mensagem;
    private RegraAereaAlteracaoRegraResponse regra;
    private RegraAereaAlteracaoCalculoResponse calculo;
    private Map<String, Object> match = new HashMap<>();
}
