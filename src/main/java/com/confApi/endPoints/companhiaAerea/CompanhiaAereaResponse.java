package com.confApi.endPoints.companhiaAerea;

import com.confApi.db.confManager.companhiaAerea.CompanhiaAerea;
import lombok.Data;

@Data
public class CompanhiaAereaResponse {
    private Integer codgCompanhiaAerea;
    private String nomeCia;
    private String iataCia;
    private String numrCia;
    private Integer status;
    private Integer flagReserva;
    private Integer flagEmitir;
    private String codgIntegracao;
    private String imgPesquisa;
    private String imgEmissao;
    private String imhLogoSmall;
    private String imgLogo;

    public CompanhiaAereaResponse(CompanhiaAerea companhiaAerea) {
        this.codgCompanhiaAerea = companhiaAerea.getCodgCompanhiaAerea();
        this.nomeCia = companhiaAerea.getNomeCia();
        this.iataCia = companhiaAerea.getIataCia();
        this.numrCia = companhiaAerea.getNumrCia();
        this.status = companhiaAerea.getStatus();
        this.flagReserva = companhiaAerea.getFlagReserva();
        this.flagEmitir = companhiaAerea.getFlagEmitir();
        this.codgIntegracao = companhiaAerea.getCodgIntegracao();
        this.imgPesquisa = companhiaAerea.getImgPesquisa();
        this.imgEmissao = companhiaAerea.getImgEmissao();
        this.imhLogoSmall = companhiaAerea.getImhLogoSmall();
        this.imgLogo = companhiaAerea.getImgLogo();
    }
}
