package com.confApi.chatconfianca.dto.request;

import lombok.Data;

@Data
public class AssumirAtendimentoRequest {
    private Long filaId;
    private Integer codgAtendente;
    private Boolean gestor;
}