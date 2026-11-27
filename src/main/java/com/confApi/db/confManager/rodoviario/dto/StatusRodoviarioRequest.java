package com.confApi.db.confManager.rodoviario.dto;
import lombok.Data;
@Data
public class StatusRodoviarioRequest {
    private Integer status;
    private String motivo;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
    private java.util.List<com.confApi.operacao.DivergenciaOperacao> divergenciasOperacao;
}
