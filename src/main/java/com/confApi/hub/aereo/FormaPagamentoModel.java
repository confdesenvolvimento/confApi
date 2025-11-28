package com.confApi.hub.aereo;

import com.confApi.db.confManager.formaPagamento.FormaPagamento;
import com.confApi.endPoints.bandeira.BandeiraResponse;
import com.confApi.endPoints.formaPagamento.FormaPagamentoResponse;

import java.io.Serializable;
import java.util.ArrayList;
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

    public FormaPagamentoModel(FormaPagamentoResponse formaPagamentoResponse) {
        this.bandeiras = new ArrayList<>();
        if(formaPagamentoResponse.getBandeiras() != null) {
            for (BandeiraResponse bandeiraResponse : formaPagamentoResponse.getBandeiras()) {
                this.bandeiras.add(new BandeiraModel(bandeiraResponse));
            }
        }
    }

    public FormaPagamentoModel(Integer codgFormaPagto, List<BandeiraModel> bandeiras) {
        super(codgFormaPagto);
        this.bandeiras = bandeiras;
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
