package com.confApi.aereo.dto;

import com.confApi.hub.aereo.dto.Voo;
import lombok.Data;

@Data
public class ClasseSelecionada {
    private String baseTarifaria;
    private String classe;
    private String familia;
    private String numero;
    private String identificacaoDeVoo;
    private Integer trecho;
    private String sistema;

    public ClasseSelecionada() {
    }

    public ClasseSelecionada(String baseTarifaria, String classe, String familia,
                             String numero, String identificacaoDeVoo, Integer trecho,
                             String sistema) {
        this.baseTarifaria = baseTarifaria;
        this.classe = classe;
        this.familia = familia;
        this.numero = numero;
        this.identificacaoDeVoo = identificacaoDeVoo;
        this.trecho = trecho;
        this.sistema = sistema;
    }

    public ClasseSelecionada(FamiliaPrecoInter familiaPrecoInter, Integer countTrecho, Trecho trecho) {
        this.baseTarifaria = familiaPrecoInter.getBaseTarifaria();
        this.classe = familiaPrecoInter.getClasse();
        this.familia = familiaPrecoInter.getFamilia().getCodgFamilia();
        this.numero = familiaPrecoInter.getNumeroVoo();
        this.identificacaoDeVoo = trecho.getFamiliaSelecionada().getIdentificacaoDeVoo();
        this.trecho = countTrecho;
        this.sistema = trecho.getSistema();
    }

    public ClasseSelecionada(Voo voo, Integer countTrecho, Trecho trecho) {
        this.baseTarifaria = trecho.getFamiliaSelecionada().getBaseTarifaria();
        this.classe = trecho.getFamiliaSelecionada().getClasse();
        this.familia = trecho.getFamiliaSelecionada().getFamilia().getCodgFamilia();
        this.numero = voo.getNumeroVoo();
        this.identificacaoDeVoo = trecho.getFamiliaSelecionada().getIdentificacaoDeVoo();
        this.trecho = countTrecho;
        this.sistema = trecho.getSistema();
    }
}
