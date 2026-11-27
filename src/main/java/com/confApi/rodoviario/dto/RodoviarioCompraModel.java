package com.confApi.rodoviario.dto;

import com.confApi.db.confManager.rodoviario.dto.SalvarReservaRodoviarioRequest;
import com.confApi.model.RecebimentoModel;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

/** Dados do fornecedor separados do recebimento processado pela Rede/Manager. */
@Getter
@Setter
public class RodoviarioCompraModel {
    private JsonNode reservaRodoviario;
    private JsonNode emissao;
    private SalvarReservaRodoviarioRequest dadosManager;
    private RecebimentoModel recebimento;
}