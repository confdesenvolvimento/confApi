package com.confApi.chatconfianca.dto.request;

import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class DepartamentoUnidadeSincronizacaoRequest {
    private Long departamentoId;
    private List<Integer> codigosUnidade = new ArrayList<>();
    private DepartamentoUnidade configuracaoPadrao;
    private Boolean replicarAtendentes;
    private Long departamentoUnidadeOrigemId;
}
