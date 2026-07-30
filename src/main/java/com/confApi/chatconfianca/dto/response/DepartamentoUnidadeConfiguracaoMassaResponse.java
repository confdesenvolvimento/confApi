package com.confApi.chatconfianca.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado da aplicacao em massa da configuracao de um departamento.
 */
@Data
@NoArgsConstructor
public class DepartamentoUnidadeConfiguracaoMassaResponse {

    private Integer totalUnidadesAtualizadas;
}
