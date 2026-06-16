package com.confApi.aereo.dto;

import com.confApi.aereo.eNums.Classe;
import com.confApi.aereo.eNums.Ordenacao;
import com.confApi.aereo.eNums.TipoBagagem;
import com.confApi.aereo.eNums.TipoTarifa;

public class ParametrosPesquisa {
    private Classe classe;
    private Ordenacao ordenacao;
    private TipoBagagem tipoBagagem;
    private TipoTarifa tarifa;

    public ParametrosPesquisa() {
    }

    public ParametrosPesquisa(Classe classe, Ordenacao ordenacao, TipoBagagem tipoBagagem, TipoTarifa tarifa) {
        this.classe = classe;
        this.ordenacao = ordenacao;
        this.tipoBagagem = tipoBagagem;
        this.tarifa = tarifa;
    }



    public Classe getClasse() {
        return classe;
    }

    public void setClasse(Classe classe) {
        this.classe = classe;
    }

    public Ordenacao getOrdenacao() {
        return ordenacao;
    }

    public void setOrdenacao(Ordenacao ordenacao) {
        this.ordenacao = ordenacao;
    }

    public TipoBagagem getTipoBagagem() {
        return tipoBagagem;
    }

    public void setTipoBagagem(TipoBagagem tipoBagagem) {
        this.tipoBagagem = tipoBagagem;
    }

    public TipoTarifa getTarifa() {
        return tarifa;
    }

    public void setTarifa(TipoTarifa tarifa) {
        this.tarifa = tarifa;
    }

    @Override
    public String toString() {
        return "ParametrosPesquisa{" +
                "classe=" + classe +
                ", ordenacao=" + ordenacao +
                ", tipoBagagem=" + tipoBagagem +
                ", tarifa=" + tarifa +
                '}';
    }
}
