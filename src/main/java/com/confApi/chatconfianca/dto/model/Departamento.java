package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Departamento {
    private Long id;
    private Long departamentoPaiId;
    private String nome;
    private String codigo;
    private String descricao;
    private String corHex;
    private String icone;
    private Boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}