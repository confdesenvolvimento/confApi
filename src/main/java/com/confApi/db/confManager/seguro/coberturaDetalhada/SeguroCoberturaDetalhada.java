package com.confApi.db.confManager.seguro.coberturaDetalhada;

import com.confApi.db.confManager.seguro.cobertura.SeguroCobertura;
import lombok.Data;

@Data
public class SeguroCoberturaDetalhada {

    private Integer codgSeguroCoberturaDetalhada;
    private SeguroCobertura seguroCobertura;
    private Integer valorCobertura;
    private String moedaCobertura;
    private Integer idCobertura;
    private String tituloCobertura;
    private String descricaoCobertura;
    private Integer idOrdemExibicao;
}
