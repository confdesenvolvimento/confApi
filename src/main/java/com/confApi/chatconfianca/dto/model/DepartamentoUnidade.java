package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.confApi.chatconfianca.dto.enums.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class DepartamentoUnidade {
    private Long id;
    private Long departamentoId;
    private Integer codgUnidade;
    private String nomeExibicao;
    private String horarioAtendimentoJson;
    private Boolean permiteChamadoAgencia;
    private Boolean permiteChamadoInterno;
    private Boolean exigeAssunto;
    private DistribuicaoDepartamento distribuicao;
    private Integer limiteChatsPorAtendente;
    private String mensagemAbertura;
    private String mensagemForaHorario;
    private Boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}