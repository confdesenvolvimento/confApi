package com.confApi.endPoints.bandeira;

import com.confApi.db.confManager.formaPagamento.FormaPagamento;
import lombok.Data;

@Data
public class BandeiraResponse {
    private Integer codgBandeira;
    private String nomeBandeira;
    private String siglaBandeira;

    public BandeiraResponse(FormaPagamento formaPagamento) {
        this.codgBandeira = formaPagamento.getCodgFormaPagto();
        this.nomeBandeira = formaPagamento.getNomeFormaPagto();
        this.siglaBandeira = null;
    }
}
