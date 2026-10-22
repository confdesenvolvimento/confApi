package com.confApi.rodoviario.dto;

import com.confApi.db.confManager.rodoviario.dto.ReservaRodoviarioDetalheDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class RodoviarioReservaOperacaoResponse {
    private boolean success;
    private String mensagem;
    private String codigo;
    private ReservaRodoviarioDetalheDTO reserva;
    private JsonNode fornecedor;
    private String codigoReservaTemporaria;
    private boolean mergeExecutado;
    private boolean statusAtualizado;
    private boolean requerConferencia;
}