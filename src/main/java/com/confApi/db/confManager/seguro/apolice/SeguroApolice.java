package com.confApi.db.confManager.seguro.apolice;

import com.confApi.db.confManager.seguro.cobertura.SeguroCobertura;
import com.confApi.db.confManager.seguro.segurado.SeguroSegurado;
import lombok.Data;

@Data
public class SeguroApolice {

    private Integer codgSeguroApolice;
    private SeguroSegurado seguroSegurado;
    private SeguroCobertura seguroCobertura;
    private Integer numeroOperacao;
    private Integer numeroApolice;
    private String paisDestino;
    private Integer endpointPdf;
}
