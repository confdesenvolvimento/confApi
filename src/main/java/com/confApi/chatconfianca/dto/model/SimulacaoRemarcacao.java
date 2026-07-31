package com.confApi.chatconfianca.dto.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimulacaoRemarcacao {
    private Long id;
    private Long conversaId;
    private String localizador;
    private Integer codgUsuario;
    private Integer codgAgencia;
    private Integer codgUnidade;
    private String status;
    private String companhiaIata;
    private Integer trechoIndice;
    private String origem;
    private String destino;
    private String trechoOriginalJson;
    private String passageirosJson;
    private String criteriosJson;
    private Long regraId;
    private String regraSnapshotJson;
    private String resultadosJson;
    private String ofertaSelecionadaJson;
    private String calculoJson;
    private Integer formaPagamentoCodigo;
    private String formaPagamentoDescricao;
    private String pagamentoStatus;
    private LocalDateTime pagamentoSelecionadoEm;
    private String motivoBloqueio;
    private LocalDateTime expiraEm;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    private Integer versao;
}
