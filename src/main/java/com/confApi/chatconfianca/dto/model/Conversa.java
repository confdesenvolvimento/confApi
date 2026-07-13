package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.confApi.chatconfianca.dto.enums.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Conversa {
    private Long id;
    private String uuid;
    private String protocolo;
    private Long departamentoUnidadeId;
    private Integer codgUnidade;
    private Integer codgAgencia;
    private Integer solicitanteCodgUsuario;
    private Integer atendenteResponsavelCodgUsuario;
    private String assunto;
    private String descricaoInicial;
    private OrigemConversa origem;
    private StatusConversa status;
    private PrioridadeConversa prioridade;
    private LocalDateTime primeiraRespostaEm;
    private LocalDateTime iniciadoEm;
    private LocalDateTime encerradoEm;
    private LocalDateTime ultimoEventoEm;
    private Integer encerradoPorCodgUsuario;
    private String motivoEncerramento;
    private String metadadosJson;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}