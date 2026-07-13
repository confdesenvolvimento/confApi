package com.confApi.chatconfianca.dto.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DashboardAtendimentoResumo {
    private Integer codgUnidade;
    private LocalDateTime atualizadoEm;
    private Long totalConversas;
    private Long abertas;
    private Long aguardandoAtendente;
    private Long emAtendimento;
    private Long aguardandoCliente;
    private Long transferidas;
    private Long encerradas;
    private Long urgentes;
    private Long slaAlerta;
    private Long slaViolado;
    private Long totalMensagens;
    private Long confiaTotal;
    private Long confiaResolvidas;
    private Long confiaAutoEncerradas;
    private Long confiaEncaminhadasHumano;
    private Long confiaEmAndamento;
    private Long confiaAvaliacoes;
    private Double confiaNotaMedia;
    private List<DashboardGrupoResumo> porDepartamento = new ArrayList<>();
    private List<DashboardGrupoResumo> porAtendente = new ArrayList<>();
    private List<DashboardGrupoResumo> confiaMotivosEncaminhamento = new ArrayList<>();
}
