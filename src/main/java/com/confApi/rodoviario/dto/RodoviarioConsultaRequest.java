package com.confApi.rodoviario.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RodoviarioConsultaRequest {
    private Integer codgReservaRodoviario;
    private JsonNode consulta;
    private String motivo;
}