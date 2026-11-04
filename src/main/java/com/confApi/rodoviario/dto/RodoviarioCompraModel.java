package com.confApi.rodoviario.dto;

import com.confApi.db.confManager.rodoviario.dto.RegistrarRecebimentoRodoviarioRequest;
import com.confApi.db.confManager.rodoviario.dto.SalvarReservaRodoviarioRequest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

/** Metadados locais separados dos requests de booking e checkout enviados ao Hub. */
@Getter @Setter
public class RodoviarioCompraModel {
    private JsonNode reservaRodoviario;
    private JsonNode emissao;
    private SalvarReservaRodoviarioRequest dadosManager;
    private RegistrarRecebimentoRodoviarioRequest recebimento;
}