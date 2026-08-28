package com.confApi.chatconfianca.intencao;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatMemoriaRecuperacaoAuditRequest {
    private Long conversaId;
    private Long mensagemId;
    private Long intencaoClassificadaId;
    private String intencaoClassificadaCodigo;
    private String statusClassificacao;
    private Integer confianca;
    private String baseAtual;
    private String statusRecuperacaoNova;
    private List<Integer> memoriasAtuais = new ArrayList<>();
    private List<Integer> memoriasNovas = new ArrayList<>();
    private Long tempoAtualMs;
    private Long tempoNovoMs;
    private String versaoRecuperador;
}
