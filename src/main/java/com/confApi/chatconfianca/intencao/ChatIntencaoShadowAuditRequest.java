package com.confApi.chatconfianca.intencao;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ChatIntencaoShadowAuditRequest {
    private Long conversaId;
    private Long mensagemId;
    private String intencaoAtual;
    private Long intencaoClassificadaId;
    private String intencaoClassificadaCodigo;
    private String statusClassificacao;
    private BigDecimal score;
    private BigDecimal segundoScore;
    private Integer confianca;
    private List<String> termosPositivos = new ArrayList<>();
    private List<String> termosNegativos = new ArrayList<>();
    private String fonte;
    private String versaoClassificador;
}
