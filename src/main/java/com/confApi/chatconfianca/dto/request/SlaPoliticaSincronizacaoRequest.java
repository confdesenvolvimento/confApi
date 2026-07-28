package com.confApi.chatconfianca.dto.request;

import com.confApi.chatconfianca.dto.enums.PrioridadeConversa;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SlaPoliticaSincronizacaoRequest {
    private Long departamentoId;
    private List<Long> departamentoUnidadeIdsEscopo = new ArrayList<>();
    private PrioridadeConversa prioridade;
    private Integer primeiraRespostaMinutos;
    private Integer resolucaoMinutos;
    private Integer alertaAntesMinutos;
    private Boolean ativo;
}
