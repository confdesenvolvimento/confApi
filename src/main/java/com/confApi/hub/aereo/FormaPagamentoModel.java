package com.confApi.hub.aereo;

import com.confApi.db.confManager.formaPagamento.FormaPagamento;

import java.io.Serializable;
import java.util.List;

public class FormaPagamentoModel extends FormaPagamento implements Serializable {

    private List<BandeiraModel> bandeiras;

    public FormaPagamentoModel(FormaPagamento fp) {
        this.setCodgFormaPagto(fp.getCodgFormaPagto());
        this.setFlagExibirFormaReserva(fp.getFlagExibirFormaReserva());
        this.setFlagExibirPagto(fp.getFlagExibirPagto());
        this.setFlagExibirReceb(fp.getFlagExibirReceb());
        this.setFlagStatus(fp.getFlagStatus());
        this.setNomeFormaPagto(fp.getNomeFormaPagto());
    }


    public List<BandeiraModel> getBandeiras() {
        return bandeiras;
    }

    public void setBandeiras(List<BandeiraModel> bandeiras) {
        this.bandeiras = bandeiras;
    }



    public FormaPagamentoModel() {
    }


    public FormaPagamentoModel(Integer codgFormaPagto) {
        this.setCodgFormaPagto(codgFormaPagto);
    }

}
