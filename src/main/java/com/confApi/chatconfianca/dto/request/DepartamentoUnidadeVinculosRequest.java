package com.confApi.chatconfianca.dto.request;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Selecao de unidades vinculadas a um departamento operacional.
 */
@Data
@NoArgsConstructor
public class DepartamentoUnidadeVinculosRequest {

    private Long departamentoId;
    private List<Integer> codigosUnidade = new ArrayList<>();
}
