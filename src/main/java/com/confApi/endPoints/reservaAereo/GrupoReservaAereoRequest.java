package com.confApi.endPoints.reservaAereo;

import lombok.Data;

@Data
public class GrupoReservaAereoRequest {
    private Boolean grupo;
    private Integer codgUsuarioSolicitante;
    private String loginUsuarioSolicitante;
}
