package com.confApi.chatconfianca.configuracao.ti;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** Internal, no-store TI content contract. Never log/serialize this object into chat metadata. */
@Getter
@Setter
public class ChatIaTiDocumento {
    private String versaoContrato;
    private String intencao, escopo;
    private Long configuracaoId, configuracaoVersao, temaId, temaVersao, perfilId, perfilVersao;
    private Long conhecimentoId, conhecimentoVersao;
    private Long etapaConhecimentoId, etapaConhecimentoVersao, etapaPerguntaId, etapaPerguntaVersao;
    private Long etapaHumanoId, etapaHumanoVersao;
    private Integer codgMemoria, editorialVersao, confiancaMinima, maxSugestoes, qtdInteracoesAtendente;
    private LocalDate vigenteDe, vigenteAte;
    private String orientacoes, restricoes, objetivo, perguntaEsclarecimento;
    private String instrucaoConhecimento, instrucaoPergunta, textoMemoria, fonte;
}
