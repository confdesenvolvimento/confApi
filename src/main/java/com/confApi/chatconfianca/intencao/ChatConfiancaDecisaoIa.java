package com.confApi.chatconfianca.intencao;

import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado unico usado pelo turno da IA para intencao, memoria, acao e roteamento.
 */
@Data
@NoArgsConstructor
public class ChatConfiancaDecisaoIa {
    private boolean unificadaHabilitada;
    private boolean canarioHabilitado;
    private boolean canarioElegivel;
    private List<String> escopoCanario = new ArrayList<>();
    private boolean aplicada;
    private String modo;
    private String status;
    private String fonte;
    private String intencao;
    private String intencaoLegada;
    private String acao;
    private String ferramenta;
    private DepartamentoUnidade departamento;
    private Integer departamentoConfianca = 0;
    private String motivo;
    private List<String> topicos = new ArrayList<>();
    private List<ChatIntencaoRuntimeDto.Memoria> memorias = new ArrayList<>();
    private ChatIntencaoClassificacao classificacaoCatalogo;
    private String statusResultado = "FALLBACK";
    private String erroCodigo;

    public boolean possuiAcao() {
        return acao != null && !acao.isBlank();
    }

    public boolean possuiFerramenta() {
        return ferramenta != null && !ferramenta.isBlank();
    }
}
