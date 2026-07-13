package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DashboardGrupoResumo {
    private String nome;
    private Long total;
    private Long abertos;
    private Long urgentes;
    private Long slaAlerta;
    private Long slaViolado;
}
