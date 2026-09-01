package com.confApi.chatconfianca.intencao;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatIaDecisaoAuditRequest {
    private Long conversaId;
    private Long mensagemId;
    private Integer codgUnidade;
    private String baseAtual;
    private Long intencaoCatalogoId;
    private String intencaoCatalogoCodigo;
    private String intencaoEfetivaCodigo;
    private String intencaoLegada;
    private String statusClassificacao;
    private Integer confianca;
    private Boolean unificadaHabilitada;
    private Boolean canarioHabilitado;
    private Boolean canarioElegivel;
    private Boolean aplicada;
    private String modo;
    private String fonte;
    private String acao;
    private String ferramenta;
    private Long departamentoSugeridoId;
    private Integer departamentoSugeridoConfianca;
    private List<Integer> memorias = new ArrayList<>();
    private Boolean sugerirAtendente;
    private Boolean atendenteSolicitado;
    private Long departamentoAtendimentoId;
    private String statusResultado;
    private Long duracaoTotalMs;
    private String erroCodigo;
    private String versaoDecisor;
}
