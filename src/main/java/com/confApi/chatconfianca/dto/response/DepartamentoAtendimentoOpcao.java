package com.confApi.chatconfianca.dto.response;

import com.confApi.chatconfianca.dto.enums.DisponibilidadeAtendimentoHumano;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DepartamentoAtendimentoOpcao {
    private Long departamentoUnidadeId;
    private Long departamentoId;
    private Integer codgUnidade;
    private String nomeExibicao;
    private String mensagemAbertura;
    private Boolean ativo;
    private Boolean possuiAtendente;
    private Boolean atendenteLivre;
    private Boolean permiteHumano;
    private Boolean somenteConfia;
    private DisponibilidadeAtendimentoHumano disponibilidadeHumano;
    private String mensagemDisponibilidade;
    private String motivoIndisponibilidade;
}
