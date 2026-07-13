package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.confApi.chatconfianca.dto.enums.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class VwConversaResumo {
    private Long id;
    private String uuid;
    private String protocolo;
    private String assunto;
    private StatusConversa status;
    private PrioridadeConversa prioridade;
    private OrigemConversa origem;
    private LocalDateTime criadoEm;
    private LocalDateTime ultimoEventoEm;
    private LocalDateTime iniciadoEm;
    private LocalDateTime encerradoEm;
    private String departamentoNome;
    private Long departamentoUnidadeId;
    private Integer codgUnidade;
    private String nomeUnidade;
    private Integer codgAgencia;
    private String nomeAgencia;
    private Integer solicitanteCodgUsuario;
    private String solicitanteNome;
    private Integer atendenteCodgUsuario;
    private String atendenteNome;
    private Long totalMensagens;
    private Long mensagensNaoLidas;
}