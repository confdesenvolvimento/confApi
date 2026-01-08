package com.confApi.model;

import com.confApi.db.confManager.formaPagamento.FormaPagamento;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
@Data
public class FormaPagamentoModel  extends FormaPagamento implements Serializable {

    private List<BandeiraModel> bandeiras;

    public FormaPagamentoModel() {

    }

    public FormaPagamentoModel(FormaPagamento fp) {
        this.setCodgFormaPagto(fp.getCodgFormaPagto());
        this.setFlagExibirFormaReserva(fp.getFlagExibirFormaReserva());
        this.setFlagExibirPagto(fp.getFlagExibirPagto());
        this.setFlagExibirReceb(fp.getFlagExibirReceb());
        this.setFlagStatus(fp.getFlagStatus());
        this.setNomeFormaPagto(fp.getNomeFormaPagto());
    }

}
