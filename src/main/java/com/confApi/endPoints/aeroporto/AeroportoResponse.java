package com.confApi.endPoints.aeroporto;

import com.confApi.hub.aereo.AeroportoHub;
import lombok.Data;

@Data
public class AeroportoResponse {
    private Integer id;
    private String codigoIata;
    private String descricao = "";

    public AeroportoResponse(AeroportoHub aeroportoHub) {
        this.id = null;
        this.codigoIata = aeroportoHub.getCodigoIata();
        this.descricao = aeroportoHub.getDescricao();
    }
}
