package com.confApi.chatconfianca.configuracao;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** Versioned metadata contract. Never transports prompts, memory texts or customer data. */
@Data
public class ChatIaConfiguracaoSnapshot {
    private String versaoContrato = "ia-config-shadow-v1";
    private Configuracao configuracao;
    private List<Etapa> etapas = new ArrayList<>();
    private List<Conhecimento> conhecimentos = new ArrayList<>();

    @Data
    public static class Configuracao {
        private Long id, versao, intencaoId, temaId, temaVersao, perfilId, perfilVersao, departamentoId;
        private String intencaoCodigo, perfilEscopo, perfilBase;
        private Integer status, intencaoStatus, temaStatus, perfilStatus, perfilUnidade;
        private Integer confiancaMinima, maxSugestoes, qtdInteracoesAtendente;
    }

    @Data
    public static class Etapa {
        private Long id, versao, acaoId, acaoVersao, acaoTemaId;
        private Integer ordem, status, acaoStatus, limiteResultados, timeoutSegundos;
        private String tipo, condicao, acaoCodigo;
        private Boolean exigirConfirmacao;
    }

    @Data
    public static class Conhecimento {
        private Long id, versao;
        private Integer codgMemoria, status, codgUnidade, prioridade, memoriaStatus, editorialUnidade, editorialVersao;
        private String escopo, base, memoriaBase, statusPublicacao;
        private Boolean permitirComplementoGeral, vinculoIntencaoAtivo, possuiTexto, possuiFonte;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalDate vigenteDe;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalDate vigenteAte;
    }
}
