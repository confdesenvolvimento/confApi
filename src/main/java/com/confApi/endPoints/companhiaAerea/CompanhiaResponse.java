package com.confApi.endPoints.companhiaAerea;

import com.confApi.hub.aereo.CompanhiaHub;
import lombok.Data;

@Data
public class CompanhiaResponse {
    private Integer id = 0;
    private String codigoIata = "";
    private String descricao = "";

    public CompanhiaResponse(CompanhiaHub companhiaHub) {
        this.id = companhiaHub.getId();
        this.codigoIata = companhiaHub.getCodigoIata();
        this.descricao = companhiaHub.getDescricao();
    }
}
