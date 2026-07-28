package com.confApi.chatconfianca.dto.request;

import com.confApi.chatconfianca.dto.enums.PapelAtendente;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class DepartamentoAtendenteSincronizacaoRequest {
    private Long departamentoId;
    private Integer codgUsuario;
    private List<Long> departamentoUnidadeIds = new ArrayList<>();
    private List<Long> departamentoUnidadeIdsEscopo = new ArrayList<>();
    private PapelAtendente papel;
    private Boolean recebeChamados;
    private Integer prioridadeDistribuicao;
    private Integer limiteChatsSimultaneos;
    private Boolean ativo;
}
