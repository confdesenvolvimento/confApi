package com.confApi.chatconfianca.intencao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatIntencaoClassificacao {
    private String status;
    private Long intencaoId;
    private String codigo;
    private String nome;
    private BigDecimal score;
    private BigDecimal segundoScore;
    private Integer confianca;
    private List<String> termosPositivos = new ArrayList<>();
    private List<String> termosNegativos = new ArrayList<>();
    private String fonte = "CHAT_INTENCAO_TERMO_V1";
    private String statusRecuperacaoMemoria = "DESABILITADO";
    private List<Integer> memoriasRecuperadas = new ArrayList<>();
    private Long tempoRecuperacaoMemoriaMs = 0L;

    public static ChatIntencaoClassificacao status(String status) {
        ChatIntencaoClassificacao resultado = new ChatIntencaoClassificacao();
        resultado.setStatus(status);
        resultado.setScore(BigDecimal.ZERO);
        resultado.setSegundoScore(BigDecimal.ZERO);
        resultado.setConfianca(0);
        return resultado;
    }

    public boolean possuiResultadoObservavel() {
        return !"DESABILITADO".equals(status);
    }
}
