package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SlaConversaResumo {
    private Long conversaId;
    private Integer primeiraRespostaMinutos;
    private Integer resolucaoMinutos;
    private Integer alertaAntesMinutos;
    private Integer minutosPrimeiraResposta;
    private Integer minutosResolucao;
    private Integer minutosRestantes;
    private Integer percentualResolucao;
    private Boolean primeiraRespostaPendente;
    private Boolean primeiraRespostaViolada;
    private Boolean resolucaoViolada;
    private Boolean emAlerta;
    private String status;
    private String label;
    private String detalhe;
}
