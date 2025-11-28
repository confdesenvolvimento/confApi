package com.confApi.endPoints.bilhete;

import com.confApi.hub.aereo.BilheteHub;
import lombok.Data;

import java.util.Date;

@Data
public class BilheteResponse {
    private Integer status;
    private String numeroBilhete;
    private Date dataEmissao;
    private Date dataCancelamento;
    private Boolean isCancelar = false;

    public BilheteResponse(BilheteHub bilheteHub) {
        this.status = Integer.parseInt(bilheteHub.getStatus());
        this.numeroBilhete = bilheteHub.getNumero();
        this.dataEmissao = bilheteHub.getDataDeEmissao();
        this.dataCancelamento = null;
        this.isCancelar = null;
    }
}
