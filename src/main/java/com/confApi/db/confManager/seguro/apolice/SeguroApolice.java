package com.confApi.db.confManager.seguro.apolice;

import com.confApi.db.confManager.seguro.cobertura.SeguroCobertura;
import com.confApi.db.confManager.seguro.segurado.SeguroSegurado;
import com.confApi.seguros.dto.SeguradoDTO;
import lombok.Data;

@Data
public class SeguroApolice {

    private Integer codgSeguroApolice;
    private SeguroSegurado seguroSegurado;
    private SeguroCobertura seguroCobertura;
    private String numeroOperacao;
    private String numeroApolice;
    private String paisDestino;
    private String endpointPdf;

    public SeguroApolice() {
    }

    public SeguroApolice(Integer codgSeguroApolice, SeguroSegurado seguroSegurado,
                         SeguroCobertura seguroCobertura, String numeroOperacao,
                         String numeroApolice, String paisDestino, String endpointPdf) {
        this.codgSeguroApolice = codgSeguroApolice;
        this.seguroSegurado = seguroSegurado;
        this.seguroCobertura = seguroCobertura;
        this.numeroOperacao = numeroOperacao;
        this.numeroApolice = numeroApolice;
        this.paisDestino = paisDestino;
        this.endpointPdf = endpointPdf;
    }

    public SeguroApolice(SeguradoDTO seguradoDTO) {
        this.codgSeguroApolice = null;
        this.seguroSegurado = null;
        this.seguroCobertura = null;
        this.numeroOperacao = seguradoDTO.getNumeroOperacao();
        this.numeroApolice = seguradoDTO.getNumeroApolice();
        this.paisDestino = seguradoDTO.getPaisDestino();
        this.endpointPdf = seguradoDTO.getEndpointPdf();
    }

    public SeguroApolice(SeguradoDTO seguradoDTO, SeguradoDTO seguradoDTORequest) {
        this.codgSeguroApolice = null;
        this.seguroSegurado = null;
        this.seguroCobertura = null;
        this.numeroOperacao = seguradoDTO.getNumeroOperacao();
        this.numeroApolice = seguradoDTO.getNumeroApolice();
        this.paisDestino = seguradoDTO.getPaisDestino();
        this.endpointPdf = seguradoDTO.getEndpointPdf();
    }
}
