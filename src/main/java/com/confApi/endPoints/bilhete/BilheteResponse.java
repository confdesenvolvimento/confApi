package com.confApi.endPoints.bilhete;

import com.confApi.aereo.eNums.StatusBilhete;
import com.confApi.aereo.eNums.StatusBilheteWooba;
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
        this.status = converterStatus(bilheteHub.getStatus());
        this.numeroBilhete = bilheteHub.getNumero();
        this.dataEmissao = bilheteHub.getDataDeEmissao();
        this.dataCancelamento = null;
        this.isCancelar = null;
    }

    private Integer converterStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return StatusBilhete.Indefinido.statusBilhete;
        }

        String statusNormalizado = status.trim();
        try {
            return Integer.parseInt(statusNormalizado);
        } catch (NumberFormatException ignored) {
            Integer statusWooba = StatusBilheteWooba.getValorPorDescricao(statusNormalizado);
            if (statusWooba != null) {
                return statusWooba;
            }

            Integer statusPadrao = StatusBilhete.getValorPorDescricao(statusNormalizado);
            return statusPadrao != null ? statusPadrao : StatusBilhete.Indefinido.statusBilhete;
        }
    }
}
